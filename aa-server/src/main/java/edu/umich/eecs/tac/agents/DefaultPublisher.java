package edu.umich.eecs.tac.agents;

import edu.umich.eecs.tac.sim.Publisher;
import edu.umich.eecs.tac.sim.Users;
import edu.umich.eecs.tac.TACAAConstants;
import edu.umich.eecs.tac.util.config.ConfigProxy;
import edu.umich.eecs.tac.user.UserEventListener;
import edu.umich.eecs.tac.auction.*;
import edu.umich.eecs.tac.props.*;
import se.sics.tasim.is.EventWriter;
import se.sics.tasim.aw.Message;
import se.sics.tasim.sim.SimulationAgent;
import se.sics.isl.transport.Transportable;

import java.util.logging.Logger;
import java.util.*;

/**
 * @author Lee Callender, Patrick Jordan
 */
public class DefaultPublisher extends Publisher implements TACAAConstants {

    private AuctionFactory auctionFactory;

    private RetailCatalog retailCatalog;

    private UserClickModel userClickModel;

    private QueryReportManager queryReportManager;

    /**
     * Query space defines the set of allowable queries
     */
    private Set<Query> querySpace;

    /**
     * The bid tracker tracks the current bids for each agent
     */
    private BidTracker bidTracker;

    /**
     * The spend tracker tracks the current spend for each agent
     */
    private SpendTracker spendTracker;

    /**
     * The bid manager tracks the bid related information for the publisher
     */
    private BidManager bidManager;

    /**
     * Configuration proxy for this publisher
     */
    private ConfigProxy publisherConfigProxy;

    /**
     * The basic auction information
     */
    private AuctionInfo auctionInfo;

    /**
     * The basic publisher info
     */
    private PublisherInfo publisherInfo;

    private Random random;

    public DefaultPublisher() {

    }

    public void nextTimeUnit(int date) {
        spendTracker.reset();
        
        //Auctions should be updated here.
        if (bidManager != null) {
            bidManager.nextTimeUnit(date);
        }
    }

    public BidManager getBidManager() {
        return bidManager;
    }

    protected void setup() {
        this.log = Logger.getLogger(DefaultPublisher.class.getName());


        random = new Random();
        
        spendTracker = createSpendTracker();

        bidTracker = createBidTracker();

        setPublisherInfo(createPublisherInfo());

        auctionFactory = createAuctionFactory();
        auctionFactory.setPublisherInfo(getPublisherInfo());


        queryReportManager = createQueryReportManager();


        
        addTimeListener(this);

        for (SimulationAgent agent : getSimulation().getUsers()) {
            Users users = (Users) agent.getAgent();
            users.addUserEventListener(new ClickMonitor());
        }
    }

    private PublisherInfo createPublisherInfo() {
        double squashingMin = getPropertyAsDouble("squashing.min", 0.0);
        double squashingMax = getPropertyAsDouble("squashing.max", 1.0);
        double squashing = squashingMin + random.nextDouble() * (squashingMax - squashingMin); 

        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setSquashingParameter(squashing);
        publisherInfo.lock();

        return publisherInfo;
    }

    private BidTracker createBidTracker() {
        BidTracker bidTracker = new BidTrackerImpl(0);

        return bidTracker;
    }

    private SpendTracker createSpendTracker() {
        SpendTracker spendTracker = new SpendTrackerImpl(0);


        return spendTracker;
    }

    private QueryReportManager createQueryReportManager() {
        QueryReportManager queryReportManager = new QueryReportManagerImpl(this, 0);

        for (String advertiser : getAdvertiserAddresses()) {
            queryReportManager.addAdvertiser(advertiser);
        }

        for (SimulationAgent agent : getSimulation().getUsers()) {
            Users users = (Users) agent.getAgent();
            users.addUserEventListener(queryReportManager);
        }

        return queryReportManager;
    }

    private BidManager createBidManager(BidTracker bidTracker, SpendTracker spendTracker) {


        BidManager bidManager = new BidManagerImpl(userClickModel, bidTracker, spendTracker);

        //All advertisers should be known to the bidManager
        String[] advertisers = getAdvertiserAddresses();
        for (int i = 0, n = advertisers.length; i < n; i++) {
            bidManager.addAdvertiser(advertisers[i]);
        }

        return bidManager;
    }

    private AuctionFactory createAuctionFactory() {
        String auctionFactoryClass = getProperty("auctionfactory.class", "edu.umich.eecs.tac.auction.LahaiePennockAuctionFactory");

        AuctionFactory factory = null;

        try {
            factory = (AuctionFactory) Class.forName(auctionFactoryClass).newInstance();
        } catch (InstantiationException e) {
            //TODO: log these
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (factory != null) {

        }

        return factory;
    }

    protected void stopped() {
        removeTimeListener(this);
    }

    protected void shutdown() {
    }

    // -------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------

    protected void messageReceived(Message message) {
        String sender = message.getSender();
        Transportable content = message.getContent();

        if (content instanceof BidBundle) {
            handleBidBundle(sender, (BidBundle) content);
        } else if (content instanceof UserClickModel) {
            handleUserClickModel((UserClickModel) content);
        } else if (content instanceof RetailCatalog) {
            handleRetailCatalog((RetailCatalog) content);
        } else if (content instanceof AuctionInfo) {
            handleAuctionInfo((AuctionInfo) content);
        }
    }

    private void handleAuctionInfo(AuctionInfo auctionInfo) {
        this.auctionInfo = auctionInfo;

        if (auctionFactory != null) {
            auctionFactory.setAuctionInfo(auctionInfo);
        }
    }

    private void handleUserClickModel(UserClickModel userClickModel) {
        this.userClickModel = userClickModel;

        bidManager = createBidManager(bidTracker, spendTracker);

        if (auctionFactory != null) {
            auctionFactory.setBidManager(bidManager);
        }
    }

    private void handleBidBundle(String advertiser, BidBundle bidBundle) {
        if (bidManager == null) {
            // Not yet initialized => ignore the RFQ
            log.warning("Received BidBundle from " + advertiser + " before initialization");
        } else {
            bidManager.updateBids(advertiser, bidBundle);

            int advertiserIndex = getSimulation().agentIndex(advertiser);
            EventWriter writer = getEventWriter();
            writer.dataUpdated(advertiserIndex, TACAAConstants.DU_BIDS, bidBundle);
        }
    }

    private void handleRetailCatalog(RetailCatalog retailCatalog) {
        this.retailCatalog = retailCatalog;

        generatePossibleQueries();


        bidTracker.initializeQuerySpace(querySpace);
    }

    private void generatePossibleQueries() {
        if (retailCatalog != null && querySpace == null) {
            querySpace = new HashSet<Query>();

            for (Product product : retailCatalog) {
                Query f0 = new Query();
                Query f1_manufacturer = new Query(product.getManufacturer(), null);
                Query f1_component = new Query(null, product.getComponent());
                Query f2 = new Query(product.getManufacturer(), product.getComponent());

                querySpace.add(f0);
                querySpace.add(f1_manufacturer);
                querySpace.add(f1_component);
                querySpace.add(f2);
            }

        }
    }

    protected String getAgentName(String agentAddress) {
        return super.getAgentName(agentAddress);
    }

    protected void sendEvent(String message) {
        super.sendEvent(message);
    }

    protected void sendWarningEvent(String message) {
        super.sendWarningEvent(message);
    }

    public void sendQueryReportsToAll() {
        if (queryReportManager != null)
            queryReportManager.sendQueryReportToAll();
    }

    

    public Auction runAuction(Query query) {
        //TODO: Check against possible queries
        if (auctionFactory != null) {
            return auctionFactory.runAuction(query);
        }

        return null;
    }

    protected class ClickMonitor implements UserEventListener {

        public void queryIssued(Query query) {
        }

        public void viewed(Query query, Ad ad, int slot, String advertiser) {
        }

        public void clicked(Query query, Ad ad, int slot, double cpc, String advertiser) {
            DefaultPublisher.this.charge(advertiser, cpc);
            spendTracker.addCost(advertiser,query,cpc);
        }

        public void converted(Query query, Ad ad, int slot, double salesProfit, String advertiser) {
        }
    }


    public void sendQueryReport(String advertiser, QueryReport report) {
        super.sendQueryReport(advertiser, report);

        int index = getSimulation().agentIndex(advertiser);

        int impressions = 0;
        int clicks = 0;

        for (int i = 0; i < report.size(); i++) {
            impressions += report.getImpressions(i);
            clicks += report.getClicks(i);
        }

        EventWriter writer = getEventWriter();
        writer.dataUpdated(index, DU_IMPRESSIONS, impressions);
        writer.dataUpdated(index, DU_CLICKS, clicks);

    }

    protected ConfigProxy getUsersConfigProxy() {
        if (publisherConfigProxy == null) {
            publisherConfigProxy = new ConfigProxy() {

                public String getProperty(String name) {
                    return DefaultPublisher.this.getProperty(name);
                }

                public String getProperty(String name, String defaultValue) {
                    return DefaultPublisher.this.getProperty(name, defaultValue);
                }

                public String[] getPropertyAsArray(String name) {
                    return DefaultPublisher.this.getPropertyAsArray(name);
                }

                public String[] getPropertyAsArray(String name, String defaultValue) {
                    return DefaultPublisher.this.getPropertyAsArray(name, defaultValue);
                }

                public int getPropertyAsInt(String name, int defaultValue) {
                    return DefaultPublisher.this.getPropertyAsInt(name, defaultValue);
                }

                public int[] getPropertyAsIntArray(String name) {
                    return DefaultPublisher.this.getPropertyAsIntArray(name);
                }

                public int[] getPropertyAsIntArray(String name, String defaultValue) {
                    return DefaultPublisher.this.getPropertyAsIntArray(name, defaultValue);
                }

                public long getPropertyAsLong(String name, long defaultValue) {
                    return DefaultPublisher.this.getPropertyAsLong(name, defaultValue);
                }

                public float getPropertyAsFloat(String name, float defaultValue) {
                    return DefaultPublisher.this.getPropertyAsFloat(name, defaultValue);
                }

                public double getPropertyAsDouble(String name, double defaultValue) {
                    return DefaultPublisher.this.getPropertyAsDouble(name, defaultValue);
                }
            };
        }

        return publisherConfigProxy;
    }


    public PublisherInfo getPublisherInfo() {
        return publisherInfo;
    }

    void setPublisherInfo(PublisherInfo publisherInfo) {
        this.publisherInfo = publisherInfo;
    }
}

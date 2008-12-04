package edu.umich.eecs.tac.auction;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.AdLink;
import edu.umich.eecs.tac.props.BidBundle.BidEntry;

import java.util.*;
import java.util.logging.Logger;

import se.sics.tasim.aw.Message;

/**
 * @author Patrick Jordan, Lee Callender
 */
public class BidManagerImpl implements BidManager {
    //TODO: Discuss 'security' issues, test, add quality score update
    //TODO: getBid, getQualityScore, etc. should remain constant throughout the day.
    private Map<String, HashMap<Query, QueryEntry> > entryMap;
    private Logger log = Logger.getLogger(BidManagerImpl.class.getName());
    private HashSet<Query> possibleQueries;
    private List<Message> bidBundleList;
  
    public BidManagerImpl() {
      entryMap = new HashMap<String, HashMap<Query, QueryEntry> >();
      bidBundleList = new ArrayList<Message>();
    }

    public void initializeQuerySpace(Set<Query> space){
      if(possibleQueries==null){
        possibleQueries = new HashSet<Query>(space);
        //TODO: Initialize default BidBundle?
      } else {
        log.warning("Attempt to re-initialize query space");
      }
  
    }

    public QueryEntry addQuery(String advertiser, Query query){   
      if(!entryMap.containsKey(advertiser)){
        return null;
      }

      if(entryMap.get(advertiser).containsKey(query)){
        return null;
      }

      //Every QueryEntry should be set to default values
      QueryEntry qe = new QueryEntry(0.0, 1.0, new AdLink(null, advertiser));
      entryMap.get(advertiser).put(query, qe);
      
      return qe;
    }

    public double getBid(String advertiser, Query query) {
      if(!entryMap.containsKey(advertiser))
        return 0.0;  //Double.NaN?
      if(!entryMap.get(advertiser).containsKey(query))
        return 0.0;
      

      
      return entryMap.get(advertiser).get(query).getBid();
    }

    public double getQualityScore(String advertiser, Query query) {
      if(!entryMap.containsKey(advertiser))
        return 1.0;
      if(!entryMap.get(advertiser).containsKey(query))
        return 0.0;
      

      return entryMap.get(advertiser).get(query).getQualityScore();
    }

    public AdLink getAdLink(String advertiser, Query query) {
      if(!entryMap.containsKey(advertiser)){
        AdLink generic = new AdLink(null, advertiser);
        return generic;
      }

      if(!entryMap.get(advertiser).containsKey(query)){
        AdLink generic = new AdLink(null, advertiser);
        return generic;  
      }
      
      AdLink ad = entryMap.get(advertiser).get(query).getAdLink();
      if(ad == null){
        ad = new AdLink(null, advertiser);
      }
      return ad;
    }

    public void updateBids(String advertiser, BidBundle bundle) {
      //Store all of the BidBundles until nextTimeUnit.
      //We'll call actualUpdateBids method there.
      Message m = new Message(advertiser,advertiser,bundle);
      bidBundleList.add(m);
    }

    public void actualUpdateBids(String advertiser, BidBundle bundle) {
      //TODO: This should be storing the bid updates, but not applying them until nextTimeUnit is called
      if(possibleQueries == null){
        log.warning("Cannot update bids because query space is un-instantiated");
        return;
      }

      if(!entryMap.containsKey(advertiser)){
        addAdvertiser(advertiser);
      }

      HashMap<Query, QueryEntry> bids = entryMap.get(advertiser);
      for (Iterator<Query> it=bundle.iterator(); it.hasNext(); ) {
        Query query = it.next();

        //TODO: Make sure query is a valid query
        if(!possibleQueries.contains(query)){
          log.warning("Unknown query "+query.toString()+" from "+advertiser);
          continue;
        }

        if(!bids.containsKey(query)){
          addQuery(advertiser, query);
        }

        //Update bid for query only if bid was specified in BidBundle
        double bid = bundle.getBid(query);
        if(bid != Double.NaN && bid >= 0.0){
          bids.get(query).setBid(bid);
        }

        //Update ad for query only if ad was specified in BidBundle
        Ad ad = bundle.getAd(query);
        if(ad != null){
          AdLink adLink = new AdLink(ad.getProduct(), advertiser);
          bids.get(query).setAdLink(adLink);
        }

      }
    }

    public Set<String> advertisers() {
        return entryMap.keySet();
    }

    public void nextTimeUnit(int timeUnit) {   
      //TODO: apply updates here
      for (Iterator<Message> it=bidBundleList.iterator(); it.hasNext(); ) {
        Message m = it.next();
        actualUpdateBids(m.getSender(), (BidBundle) m.getContent());  
      }
      bidBundleList.clear();
      
    }

    public void addAdvertiser(String advertiser) {
        if(!entryMap.containsKey(advertiser)) {
            entryMap.put(advertiser, new HashMap<Query, QueryEntry>());
        }

    }

    private static class QueryEntry {
        //TODO: track the attributes for each query
        private double bid;
        private double qualityScore;
        private AdLink ad;

        public QueryEntry(){
          bid = 0.0;
          qualityScore = 1.0;
        }

        public QueryEntry(double bid, double qualityScore, AdLink ad){
          setBid(bid);
          setQualityScore(qualityScore);
          setAdLink(ad);
        }

        public AdLink getAdLink() {
            return ad;
        }

        public void setAdLink(AdLink ad) {
            this.ad = ad;
        }

        public double getBid() {
            return bid;
        }

        public void setBid(double bid) {
            this.bid = bid;
        }

        public double getQualityScore(){
            return qualityScore;
        }

        public void setQualityScore(double score){
            this.qualityScore = score; 
        }
        
    }
}
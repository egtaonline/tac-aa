package edu.umich.eecs.tac.props;

import se.sics.isl.transport.Context;
import se.sics.isl.transport.ContextFactory;
import se.sics.tasim.props.*;

/**
 * @author Lee Callender 
 */
public class AAInfo implements ContextFactory{
    public static final String CONTEXT_NAME = "aacontext";

    /** Cache of the last created context (since contexts should be constants) */
    private static Context lastContext;

        // Prevent instances of this class
    public AAInfo() {
    }

    public Context createContext() {
      return createContext(null);
    }

    public Context createContext(Context parentContext) {
      Context context = lastContext;
      if (context != null && context.getParent() == parentContext) {
        return context;
      }

      context = new Context(CONTEXT_NAME, parentContext);
      // New in version 0.9.7
      context.addTransportable(new Ping());
      // New in version 0.9.6
      //context.addTransportable(new ActiveOrders());

      context.addTransportable(new Alert());
      context.addTransportable(new BankStatus());
      context.addTransportable(new BidBundle());
      context.addTransportable(new QueryReport());
      context.addTransportable(new BidBundle());
      context.addTransportable(new SimulationStatus());
      context.addTransportable(new StartInfo());
      context.addTransportable(new ServerConfig());
      context.addTransportable(new Query());
      context.addTransportable(new SalesReport());
      context.addTransportable(new SalesReport.SalesReportEntry());
      context.addTransportable(new QueryReport());
      context.addTransportable(new QueryReport.QueryReportEntry());

      // Cache the last context
      lastContext = context;
      return context;
    }

  } // AAInfo


package uk.co.gockett.ipbtools.topology;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Description: Main class for topol project
 * @author ruanoj@github
 */
public class Main implements DiscoveryListener, Runnable {

  private final Object lock= new Object();
  @SuppressWarnings("unused")
  private int workers = 0;
  private int idle = 0;
  private int inProgress = 0;
  private int completed = 0;
  private int errored = 0;
  private boolean finished = false;

  public static void main(String[] args) {

    final String DEFAULT_FIRST_NODE = "1.1.1.1";
    String firstNode = DEFAULT_FIRST_NODE;
    if ( args.length > 0) {
      firstNode = args[0];
    }
    Router node = null;
    try {
      InetAddress add = InetAddress.getByName(firstNode);
      firstNode = add.getHostAddress();
      node = new Router(add);
      RouterStorage.getAdjStorage().newAdjacency( firstNode, node );
    } catch (UnknownHostException e) {
      System.err.println("Invalid IP/unknown hostname");
      System.exit(1);
    }

    Main main = new Main();
    new Thread(main).start();
    new Thread(new AdjDiscoveryTask(main)).start();
    new Thread(new AdjDiscoveryTask(main)).start();
    /*
    new Thread(new AdjDiscoveryTask(main)).start();
    new Thread(new AdjDiscoveryTask(main)).start();
    new Thread(new AdjDiscoveryTask(main)).start();
    */

    // Common thread waiting for finished flag
    while(!main.finished) {
      try {
        Thread.sleep(1000);
      } catch(InterruptedException ex) {} // ignore
    }
    // Print out
/*System.out.println(
        "Hold:"+main.idle+
        ", Pending:"+main.inProgress+
        ", Done:"+main.completed+
        ", Error:"+main.errored);*/

    // Wait for inProgress variable to get to zero
    // (set a timeout just in case any of the threads died
    // unexpectedly)
    while(main.inProgress!=0) {
      try {
        Thread.sleep(500);
      } catch(InterruptedException ex) {} // ignore
    }

    // Header
    System.out.println("# Topology output");
    System.out.println("# " + new Date());

    // XXX quick hack to allow filtering
    Hashtable<String, String> shadowDevices = new Hashtable<String, String>(30);

    // Dump router/hostname list
    System.out.println("# HOSTNAME");
    Iterator<?> it = RouterStorage.getAdjStorage().getData();
    while(it.hasNext()) {
      Router r = (Router)it.next();
      String hostname = r.getHostname();
      String hostaddress = r.getHostAddress();
      // This if clause adds shadow routers to filter list
      if (hostname.matches("^.+-shadow-.+$")) {
        shadowDevices.put(hostaddress, hostname);
      }
      System.out.println("# "+hostaddress+":"+hostname);
//            System.out.println("# "+r.getHostAddress()+":"+r.getHostname());
    }

    System.out.println("# ");

    // Dump adjacency list
    System.out.println("# DATA");
    it = RouterStorage.getAdjStorage().getData();
    while(it.hasNext()) {
      Router r = (Router)it.next();
      System.out.println("# "+r.getHostAddress()+" ("+r.getHostname()+")");
      Iterator<?> it2 = r.getAdjacencies().iterator();
      while(it2.hasNext()) {
        Adjacency adj = (Adjacency)it2.next();
        // The if clause avoids adjacencies to shadow routers to appear
        if (! shadowDevices.containsKey(adj.getAdjacentAddress()))
          System.out.println(r.getHostAddress()
                        +":"+adj.getAdjacentAddress()
                        +":"+adj.getRouterInterface()
                        +":"+(1000*adj.getSpeed())
                        );
      }
    }
  }

  public void join() {
    synchronized(lock) {
      workers++;
      idle++;
    }
  }

  public void leave() {
    synchronized(lock) {
      idle--;
      workers--;
    }
  }

  /*public void idle() {
    synchronized(lock) {
      idle++;
    }
  }*/

  public synchronized void working() {
    synchronized(lock) {
      idle--;
      inProgress++;
    }
  }

  public synchronized void successResult() {
    synchronized(lock) {
      inProgress--;
      completed++;
      idle++;
    }
  }

  public synchronized void errorResult() {
    synchronized(lock) {
      inProgress--;
      errored++;
      idle++;
    }
  }

  /**
   * This method will print out status from time to time, and will
   * also detect a first-node-error status, and also the end of the
   * topology discovery.
   */
  public void run() {
    int st = 0;
    while(true) {
      if (completed==0 && errored==1) {
        // This means the first node queried was an error.
        // Critical situation, we need to finish
        System.err.println("Node specified could not be queried. Bailing out.");
        new AdjDiscoveryTask(true);
        break;
      }
      if (inProgress==0 && completed>0) {
        // This means there are no more nodes to be queried, nor
        // nodes currently being treated. This marks the end of
        // the discovery
        System.err.println("End of discovery");
        System.err.println("Nodes succesfully queried:" + completed);
        System.err.println("Nodes not responding:" + errored);
        new AdjDiscoveryTask(true);
        break;
      }
      if (st++ > 2) {
        st = 0;
        synchronized(lock) {
          System.err.println("Idle:"+idle+", Querying:"+inProgress+", Nodes completed:"+completed+", errors:"+errored);
        }
      }
      // Testing. When 10 nodes have been processed, we will shutdown
      /*if (completed>=20) {
        System.err.println("At least 20 nodes completed. Testing finished.");
        new AdjDiscoveryTask(true);
        break;
      }*/
      try {
        Thread.sleep(1000);
      } catch(InterruptedException ex) {} // ignore
    }
    finished=true;
  }
}

package uk.co.gockett.ipbtools.topology;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

/**
 * AdjDiscoveryTask: Threaded task meant to retrieve pending nodes and populate
 * their adjacency nodes.
 *
 * TODO: Notification of operations done. This will enable to a) count the
 * number of routers queried,and b) detect whether first router answers (if
 * first router is not reachable/does not reply, the threads will wait for
 * new nodes forever.) Currently, boolean 'finished' used.
 * 
 * @author ruanoj@github
 */
public class AdjDiscoveryTask implements Runnable {

    private final static long SLEEP_TIME = 1000L;  // Pause between checks for pending nodes
    public static boolean finished = false; // See todo

    private static int instanceid = 1;      // Internal instance counter for class
    private final int here = instanceid++;  // # of this instance

    // router storage: XXX Could this be static?
    private final RouterStorage storage = RouterStorage.getAdjStorage();
    private AdjDiscoverer discoverer = new AdjDiscoverer();
    private DiscoveryListener status;

    AdjDiscoveryTask(DiscoveryListener status) {
        this.status = status;
    }

    // Method that will make other objects to exit upon checking for
    // finished status.
    AdjDiscoveryTask(boolean finish) {
        finished = finish;
    }

    public void run() {
        Router node;
        String anAdjacency;
        Map<?, ?> found;
        
        status.join();
        // loop run
        do {
            node = null;
            while(node == null) {
                // loop get
                //  if finished then return
                if (finished) {
                    status.leave();
                    return;
                }
                //  get pending node
                node = (Router) storage.getPendingAdj();
                //  if no new node then pause
                if (node == null) {
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch(InterruptedException e) { } // Ignored
                }
            // end loop get
            }

            /*
             * Originally, adjacencies were received here and added to
             * the router node, but I decided to delegate this latter
             * process to the AdjDiscoverer object itself.
             *
             * The resulting solution is less loosely coupled, but we
             * save code in encoding the results, return them, parse
             * them and add the adjacencies.
             *
             */
//            outprintln("Processing node " + node.getHostAddress());
            status.working();

            String hostname = discoverer.getHostname(node);
            if (hostname !=null && hostname.matches("^.+-shadow-.+$")) {
                // Ignoring shadow routers
//                errprintln("Ignoring shadow router:" + hostname);
                // XXX mark node as ignored;
                status.successResult();
                continue;
            }
            node.setHostname(hostname);
            
            found = discoverer.findAdjacencies(node);
            if (found==null) {
                errprintln("Error while finding adjacencies.");
                status.errorResult();
                continue;
            }

            // Add newly discovered adjacencies as nodes to storage
            Iterator<?> it = found.keySet().iterator();
            while (it.hasNext()) {
                anAdjacency = (String)it.next();
                try {
                    Router newnode = new Router(InetAddress.getByName(anAdjacency));

                    /*// Query node for hostname
                    String hn = discoverer.getHostname(newnode);    // This resets target ip address
                    if (hn == null) {
                    // Get
                    hn = newnode.getHostname();
                    }
                    if (hn.matches("^.+-shadow-.+$")) {
                    continue;
                    // Ignoring shadow routers
                    //                        System.out.println("(Ignoring  )"+hn);
                    }*/
                    
                    if (storage.newAdjacency(anAdjacency, newnode)) {
//                        System.out.println("(Discovered)"+hn);
                    } else {
                        // XXX: Error, need to throw something
                    }

                } catch(UnknownHostException ex) {} // ignore
            }
            // inform of current node completeness

            // if not finished then repeat loop
          // end loop run
//            outprintln("finished node " + node.getHostAddress());
            status.successResult();
        } while( !finished );
//        outprintln("getting out of run()");
        status.leave();
    }

    private void print(PrintStream ps, String text ) {
        ps.print(here + ": " + text);
    }

    @SuppressWarnings("unused")
	private void outprintln(String text) {
        print(System.out, text + "\n");
    }

    @SuppressWarnings("unused")
	private void outprint(String text) {
        print(System.out, text);
    }

    private void errprintln(String text) {
        print(System.err, text + "\n");
    }
}

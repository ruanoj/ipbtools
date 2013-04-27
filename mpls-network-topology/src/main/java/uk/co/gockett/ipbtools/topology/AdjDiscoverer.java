package uk.co.gockett.ipbtools.topology;

import java.util.Hashtable;
import java.util.Map;

/**
 * AdjDiscoverer is the central class for router discovery.
 * 
 * TODO: Implement file-based cache
 * @author ruanoj@github
 */
public class AdjDiscoverer {

    public final static String DEFAULT_COMMUNITY = "public";

    private final SNMPQuery query = new SNMPQuery();

    public AdjDiscoverer() {
        // TODO: Retrieve specified community via command line arguments
        query.setCommunity(DEFAULT_COMMUNITY);
    }

    public Map<String, Adjacency> findAdjacencies( Router node ) {
        Hashtable<String, Adjacency> result = null;
        // Set address
        if (query.setAddress(node.getInetAddress()) == false) {
            return null;
        }
        // Ask SNMPQuery to set adjacencies
        result = query.getAdjacencies();

        if (result != null) {
            node.addAllAdjacencies(result);
        }

        //System.out.println(node);
        return result;
    }

    public String getHostname(Router node) {
        if (query.setAddress(node.getInetAddress()) == false) {
            return null;
        }
        return query.getHostname();
    }
}

package uk.co.gockett.ipbtools.topology;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Collection;

/**
 * Holds relevant information for a given router (the definition 'router'
 * may become 'network device' in the future)
 * @author ruanoj@github
 */
public class Router {
    final private int HASHTABLE_DEFAULT_INITIAL_CAPACITY = 50;
    final private String DEFAULT_COMMUNITY = "public";
    private InetAddress address;
    private String community;   // SNMP community
    private String hostname;    // Host name
    private Hashtable<String, Adjacency> adjacencies;   // Routers to which this one is adjacent to
                                // key should be interface
                                // value the adjacent Router instance

    public Router( InetAddress address ) {
        this.address = address;
        this.hostname = null;
        this.community = DEFAULT_COMMUNITY;
        this.adjacencies = new Hashtable<String, Adjacency>(HASHTABLE_DEFAULT_INITIAL_CAPACITY);
    }

    public InetAddress getInetAddress() {
        return address;
    }

    public String getHostAddress() {
        return address.getHostAddress();
    }

    public void setHostname( String hostname ) {
        if (hostname!=null) {
            this.hostname = hostname;
        }
    }

    public String getHostname() {
        if (hostname == null) {
            hostname = address.getCanonicalHostName();
        }
        return hostname;
    }

    public void setCommunity( String community ) {
        this.community = community;
    }

    public String getCommunity() {
        return community;
    }

    /*
     * TODO: Set remoteInterface to both physical and snmp interfaces,
     *  separated by a colon, e.g. xe-5/0/0:901
     */
    public void addAdjacency(String remoteInterface, Adjacency neighbor) {
        if (adjacencies.containsKey(remoteInterface)) {
            System.err.println("Warning: Adjacency \"" + remoteInterface +"\" in router "+ getInetAddress() + " already exists.");
        }
        adjacencies.put(remoteInterface, neighbor);
    }

    public void addAllAdjacencies(Map<String, Adjacency> t) {
        adjacencies.putAll(t);
    }

    public Collection<Adjacency> getAdjacencies() {
        return adjacencies.values();
    }
    // XXX Do we need something more elaborate here?
    public String toString() {
        StringBuffer value = new StringBuffer("Router["+address.toString()+", community:"+community+", \n adjacencies[\n");
        Enumeration<String> en = adjacencies.keys();
        while(en.hasMoreElements()) {
            String key = (String)en.nextElement();
            Adjacency adj = (Adjacency)adjacencies.get(key);
            value.append(" ["+key+"]->("+adj.toString()+")\n");
        }
        value.append(" ]]");
        return new String(value);
    }
}

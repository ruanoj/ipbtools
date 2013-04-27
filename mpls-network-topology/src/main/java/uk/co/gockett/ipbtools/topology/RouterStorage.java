package uk.co.gockett.ipbtools.topology;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Singleton that deals directly with adjacency data.
 * Singleton
 * @author ruanoj@github
 */
public class RouterStorage {
    public final static int DISCOVERED_INITIAL_CAPACITY = 20;
    private static RouterStorage theStorage = null;
    private final static Object lock = new Object();

    /* discovered shows all the nodes already seen, either as a primary
     * argument, or by being an adjacency of another node. Index is its
     * IP address on a String
     */
    private static Hashtable<String, Object> discovered;
    /* pending contains the nodes yet to be queried
     */
    private static List<Object> pending;
    /* data contains the data itself. This container could be merged
     * with 'discovered'
     */
    private static List<Object> data;


    public static RouterStorage getAdjStorage() {
        synchronized (lock) {
            if (theStorage == null) {
                theStorage = new RouterStorage();
            }
        }
        return theStorage;
    }

    public synchronized Object getPendingAdj() {
        if (pending.isEmpty() == true) {
            return null;
        }
        return pending.remove(0);
    }

    /* Inform of a new adjacency
     * A new adjacency has been discovered. This will trigger its inclusion
     * on the list of pending nodes to be queried, and also as new data.
     * XXX: Will this work? equals vs. == for the String object
     */
    public synchronized boolean newAdjacency(String str, Object obj) {
        if( discovered.containsKey(str) == true) {
            return false;
        }
        discovered.put(str, obj);
        pending.add(obj);
        data.add(obj);
        return true;
    }

    /**
     * Iterator to access data
     * XXX Do we really mind whether this gives write access to stored objects?
     * @return
     */
    public Iterator<Object> getData() {
        return data.iterator();
    }
    private RouterStorage() {
        // discovered nodes will be looked up to find out whether
        // we already know about nodes as they appear in the
        // adjacency list of traversed ones.
        discovered = new Hashtable<String, Object>(DISCOVERED_INITIAL_CAPACITY);
        // pending nodes, and data about themselves are just
        // containers that will not be looked up, just first-element
        // retrieval (pending), and adding new nodes (data).
        pending = Collections.synchronizedList(new LinkedList<Object>());
        data = Collections.synchronizedList(new LinkedList<Object>());
    }
}

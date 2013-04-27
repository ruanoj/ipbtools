package uk.co.gockett.ipbtools.topology;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.RetrievalEvent;
import org.snmp4j.util.TableUtils;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.DefaultPDUFactory;


/**
 * It encapsulates SNMP-related queries and data, both to Juniper and Cisco
 * routers.
 *
 * TODO: Get snmp default SNMP community from properties
 * TODO: getAdjacencies accepts a Router parameter, so adjacencies are
 *  filled in from this class.
 * 
 * @author ruanoj@github
 */
public class SNMPQuery {

    final public static String DEFAULT_COMMUNITY = "public";
    final public static int DEFAULT_UDP_PORT = 161;
    final public static int DEFAULT_TIMEOUT = 5000;

    // SNMPv2-MIB::sysName.0
    final private static String SYS_NAME = "1.3.6.1.2.1.1.5.0";
    // SNMPv2-MIB::sysDescr.0
    final private static String SYS_DESCR = "1.3.6.1.2.1.1.1.0";
    // JUNIPER-MPLS-LDP-MIB::jnxMplsLdpHelloAdjType = 1(link), 2(targeted)
    final private static String JNX_LDP_ADJ = "1.3.6.1.4.1.2636.3.36.1.3.5.1.1.3";
    final private static String JNX_DESCR = "Juniper";
    // MPLS-LDP-MIB::mplsLdpHelloAdjacencyType = 1(link), 2(targeted)
    final private static String IOS_LDP_ADJ = "1.3.6.1.4.1.9.10.65.1.3.2.1.1.3";
    final private static String IOS_DESCR = "Cisco";
    // RFC1213-MIB::ipRouteifIndex. - Prefix to retrieve ifIndex out of adjacencies - only cisco
    final private static String prefixOIDRfc1213IpRouteIndex = "1.3.6.1.2.1.4.21.1.2.";
    final private static int indexOIDRfc1213IpRouteIndex = 10;  // index where IP address will be
    // other cisco-only stuff - new approach
    final private static String OIDMplsLdpSesState = "1.3.6.1.4.1.9.10.65.1.3.4.1.1";
    final private static int indexLinkOIDMplsLdpSesState = 6; //20;  // link address - 32-bit integer
    final private static int indexLoopbackOIDMplsLdpSesState = 7; //21;  // loopback address - 4 integers
    //
    final private static String OIDMplsLdpEntityTargetedPeer = "1.3.6.1.4.1.9.10.65.1.2.2.1.17";
    final private static int indexOIDMplsLdpEntityTargetedPeer = 6; //20; // link address - 32-bit integer
    //
    final private static String OIDMplsLdpEntityConfGenIfIndxOrZero = "1.3.6.1.4.1.9.10.65.1.2.3.1.1.3";
    final private static int indexOIDMplsLdpEntityConfGenIfIndxOrZero = 6; // link address - 32-bit integer
    //
    final private static String prefixOIDRfc1213IpRouteNextHop = "1.3.6.1.2.1.4.21.1.7.";
    final private static int indexOIDRfc1213IpRouteNextHop = 10;
    //
    final private static String prefixOIDRfc1213IpRouteIfIndex = "1.3.6.1.2.1.4.21.1.2.";
    //
    final private static String prefixOIDRfc1213IpRouteMetric1 = "1.3.6.1.2.1.4.21.1.3.";
    //
    // other cisco-only stuff - end of new approach
    // IF-MIB::ifDescr.
    final private static String prefixOIDIfDescr = "1.3.6.1.2.1.2.2.1.2.";
    final private static int indexOIDIfDescr = 10;  // index where ifIndex will be
    // IF-MIB::ifHighSpeed
    final private static String prefixOIDIfHighSpeed ="1.3.6.1.2.1.31.1.1.1.15.";


    // Variables for LDP Hello Adjacency OIDs
    final private static OID jnxMplsLdpHelloAdjTypeOID = new OID(JNX_LDP_ADJ);
    final private static OID mplsLdpHelloAdjacencyTypeOID = new OID(IOS_LDP_ADJ);

    // Variables for snmp4j.util.TableUtils.getTable()
    final private OID [] argOID = new OID[1];
    final private static OID OIDLowerBound = new OID("0");
    final private static OID OIDUpperBound = new OID("256");

    @SuppressWarnings("unused")
	private InetAddress address = null;
    private String s_address = null;
    private CommunityTarget target = new CommunityTarget();
    private String s_community;
    private Snmp snmp = null;               // SNMP session object
    private TableUtils table = null;        // TableUtils instance

    private boolean isCisco = false;        // Cisco hardware
    private boolean unknownArch = true;     // Unknown architecture

    public SNMPQuery(String community) {
        target.setTimeout(DEFAULT_TIMEOUT);
        target.setCommunity(new OctetString(community));
        s_community = community;
    }

    public SNMPQuery() {
        this(DEFAULT_COMMUNITY);
    }

    public void setCommunity(String community) {
        if (s_community !=null && ! s_community.equals(community)) {
            s_community = community;
            target.setCommunity(new OctetString(community));
        }
    }

    public String getCommunity() {
        return s_community;
    }

    /*
     * Sets address for snmp query, accepting a string parameter.
     * This method just converts it into an InetAddress and then
     * calls setAddress(InetAddress)
     *
     * @return boolean with the success/failure of operations
     * 
     */
    public boolean setAddress(String address) {
        try {
            InetAddress inet = InetAddress.getByName(address);
            if (setAddress(inet) == false) {
                System.err.println("setAddress: Error while setting address");
                return false;
            }
        } catch (UnknownHostException ex) {
            System.err.println("setAddress: Invalid hostname/IP address: " + address);
            return false;
        }
        return true;
    }

    /*
     * Sets address for snmp query. To check the device is reachable
     * and community is correct, it queries sysDescr to find out about
     * device's manufacturer.
     *
     * @return boolean with the success/failure of operation
     *
     */
    public boolean setAddress(InetAddress inet) {
        try {
            target.setAddress(new UdpAddress(inet, DEFAULT_UDP_PORT));
            address = inet;
            s_address = inet.getHostAddress();
            if (snmp == null) {
                snmp = new Snmp(new DefaultUdpTransportMapping());
                snmp.listen();
            }
            if (table == null) {
                table = new TableUtils(snmp, new DefaultPDUFactory());
            }

            // Gets hostname, finds architecture (whether Cisco or Juniper)
            getPersonality();

        } catch (IOException e) {
            System.err.println("setAddress: IOException while creating Snmp object");
            e.printStackTrace();
            snmp = null;
            return false;
        }
        return true;
    }

    public String getAddress() {
        return s_address;
    }

    public String getString(String oid) {
        ResponseEvent re = get( oid );
        PDU pdu = re.getResponse();
        if (pdu == null) {
            return null;
        }
        String s = pdu.get(0).getVariable().toString();
        System.out.println("Result: " + s);
        return s;
    }

    public int getInt(String oid) {
        ResponseEvent re = get( oid );
        PDU pdu = re.getResponse();
        if (pdu == null) {
            return -1;
        }
        int i = pdu.get(0).getVariable().toInt();
        return i;
    }

    private ResponseEvent get(String oid) {
        ResponseEvent re = null;
        VariableBinding vb = new VariableBinding(new OID(oid));
        PDU pdu = new PDU();
        pdu.add(vb);
        try {
            re = snmp.get(pdu, target);
        } catch(IOException e) { } // Ignore
        return re;
    }

    public String getHostname() {
        ResponseEvent re = get( SYS_NAME );
        PDU pdu = re.getResponse();
        if (pdu == null) {
            // Query timed out
            System.err.println("getHostname: Snmp query timed out.");
            return null;
        }
        String hostname = pdu.get(0).getVariable().toString();
        return hostname;
    }

    /* Retrieves basic info from device
     *
     * @return True, if target was contacted. False, if any error happened
     */
    private boolean getPersonality() {
        ResponseEvent re = get( SYS_DESCR );
        PDU pdu = re.getResponse();
        if (pdu == null) {
            // Query timed out
            System.err.println("findArchitecture: Snmp query timed out.");
            return false;
        }

        // Reset value
        unknownArch = false;

        String sysdescr = pdu.get(0).getVariable().toString();
        if (sysdescr.startsWith(IOS_DESCR)) {
            //oid_adjacency = new OID(IOS_LDP_ADJ);
            isCisco = true;
        } else if (sysdescr.startsWith(JNX_DESCR)) {
            //oid_adjacency = new OID(JNX_LDP_ADJ);
            isCisco = false;
        } else {
            System.err.println("findArchitecture: Unknown hardware.");
            unknownArch = true;
            return false;
        }
        return true;
    }

    /**
     * Version Cisco to obtain adjacencies.
     * First piece of code is similar (except for OID). The SNMP interface
     * information has to be retrieved from a second query.
     *
     * The list of adjacent nodes is obtained by querying the SNMP variable
     * jnxMplsLdpHelloAdjType (Juniper) or mplsLdpHelloAdjacencyType (Cisco).
     * This query returns the LDP Hello adjacencies, and the function takes
     * the ones that have been seen via 'Link' (value 1).
     *
     * On Cisco devices we need to perform an additional query
     * (IP-MIB::ipRouterifIndex.IP.IP.IP.IP) to obtain the SNMP ifIndex.
     */
    private Hashtable<String, Adjacency> getAdjacenciesCisco() {

        System.err.println("Retrieving adjacencies for node "+target.getAddress());
        // 0 - Retrieve hash of link->loopback association of all operational LDP sessions
        System.err.println("STEP0:");
        argOID[0] = new OID(OIDMplsLdpSesState);
        List<?> lst = table.getTable(target, argOID, OIDLowerBound, OIDUpperBound);
        if (((RetrievalEvent)lst.get(0)).getStatus() != RetrievalEvent.STATUS_OK) {
            // Timeout
            System.err.println("getTable: SNMP query timed out.");
            return null;
        }

        // [ Long linkAddress ] = String ldpPeer
        Hashtable<Long, String> linkToLoopback = new Hashtable<Long, String>(10);

        // Here are the results. We are only interested in the
        // LDP sessions that are operational(5)
        ListIterator<?> it = lst.listIterator();
        while(it.hasNext()) {
            TableEvent ev = (TableEvent)it.next();

            // cisco: node queried -> AA.BB.CC.DD (loopback address)
            // org.snmp4j.util.TableEvent[
            //       localLoopback       remotelinkip  remoteLoopback
            //                           INTERFACE-IP
            // index=(AA.BB.CC.DD).0.0.(1424602962).(EE.FF.GG.HH).0.0.1,
            // vbs=[1.3.6.1.4.1.9.10.65.1.3.2.1.1.3.AA.BB.CC.DD.0.0.1424602962.EE.FF.GG.HH.0.0.1 = 1],
            // status=0,exception=null,report=null]

            // This is the value for the OID
            int sesState = ev.getColumns()[0].getVariable().toInt();
            if (sesState == 5) { // operational
                OID oidIndex = ev.getIndex();
                int [] oid_index = oidIndex.getValue();
                // Operational LDP session - link address
                Long linkAddress = new Long(oidIndex.getUnsigned(indexLinkOIDMplsLdpSesState)); // JDK1.4
                // Operational LDP session - loopback, or LDP ident
                String ldpPeer = IPUtils.intArrayToIp(oid_index, indexLoopbackOIDMplsLdpSesState);
                System.err.println("Operational LDP session: " + ldpPeer
                        + ", (link/"+linkAddress+"/"+IPUtils.longToIp(linkAddress)+")");

                linkToLoopback.put(linkAddress, ldpPeer);
            }
        }

        // 1 - Obtain non-targeted LDP peers (link only)
        System.err.println("STEP1:");
        argOID[0] = new OID(OIDMplsLdpEntityTargetedPeer);
        lst = table.getTable(target, argOID, OIDLowerBound, OIDUpperBound);
        if (((RetrievalEvent)lst.get(0)).getStatus() != RetrievalEvent.STATUS_OK) {
            System.err.println("getTable: SNMP query timed out.");
            return null;
        }

        // [ Long linkAddress ] = Integer ifNumber
        Hashtable<Long, Integer> notTargeted = new Hashtable<Long, Integer>(10);

        it = lst.listIterator();
        while( it.hasNext()) {
            TableEvent ev = (TableEvent)it.next();
            // This is the value for the OID
            int targetedPeer = ev.getColumns()[0].getVariable().toInt();
            if (targetedPeer == 2) { // not targeted

                //System.err.println(ev);
                OID oidIndex = ev.getIndex();
                // Operational LDP session - link address
                Long linkAddress = new Long(oidIndex.getUnsigned(indexOIDMplsLdpEntityTargetedPeer)); // JDK1.4
                // Operational LDP session - loopback, or LDP ident
                System.err.println("Non-targeted LDP session: x.x.x.x" +
                        ", (link/"+linkAddress+"/"+IPUtils.longToIp(linkAddress)+")");

                notTargeted.put(linkAddress, new Integer(0)); // JDK1.4
            }
        }

        // 2 - Get ifNumber for results of previous step
        System.err.println("STEP2:");
        argOID[0] = new OID(OIDMplsLdpEntityConfGenIfIndxOrZero);
        lst = table.getTable(target, argOID, OIDLowerBound, OIDUpperBound);
        if (((RetrievalEvent)lst.get(0)).getStatus() != RetrievalEvent.STATUS_OK) {
            // Timeout
            System.err.println("getTable: SNMP query timed out.");
            return null;
        }
        it = lst.listIterator();
        while( it.hasNext()) {
            TableEvent ev = (TableEvent)it.next();
            // This is the value for the OID
            int ifIndex = ev.getColumns()[0].getVariable().toInt();
            if (ifIndex != 0) { // not targeted
                OID oidIndex = ev.getIndex();
                // Operational LDP session - link address
                Long linkAddress = new Long(oidIndex.getUnsigned(indexOIDMplsLdpEntityConfGenIfIndxOrZero)); // JDK1.4
                if (notTargeted.containsKey(linkAddress)) {
                    notTargeted.put(linkAddress, new Integer(ifIndex));
                    System.err.println("(link/"+linkAddress+"/"+IPUtils.longToIp(linkAddress)+") = ifIndex "+ifIndex);
                }
            }
        }


        // 3 - For operational LDP sessions that are targeted, iterate and find out
        //     the ones with the lowest metric, cause those are direct adjacencies
        System.err.println("STEP3:");
        PDU routePdu = new PDU();

        Enumeration<Long> en = linkToLoopback.keys(); // Iterating thru link addresses
        while(en.hasMoreElements()) {
            Long key = (Long)en.nextElement();
            if (! notTargeted.containsKey(key)) {
                // This targeted may be a valid hello adjacency
                String stLoop = (String)linkToLoopback.get(key);    // LDP peer
                System.err.println("Targeted LDP session "+stLoop+" may be adjacent.");
                routePdu.add(new VariableBinding(new OID(prefixOIDRfc1213IpRouteNextHop + stLoop)));
            }
        }

        // XXX make query and, grouping by next hop, get the adjacency with the
        //     lowest metric. Query ipRouteIndex to find on the next hop to get
        //     ifIndex
        //
        // Only if next hop is not in nonTargeted list, means that is a
        // previously unknown adjacency

        if (routePdu.size() != 0)
        try {
            System.err.println("Retrieving ipRouteNextHop ");

            ResponseEvent routeRev = snmp.get(routePdu, target);
            PDU routeResPdu = routeRev.getResponse();

            if (routeResPdu == null) {
                System.err.println("getAdjacenciesCisco: snmp timeout");
                return null;
            }

            // nexthop means the same as link address
            Hashtable<Long, Integer> candidateMetric = new Hashtable<Long, Integer>(10);      // [Long nexthop] = Integer metric1
            Hashtable<Long, String> candidateLoopback = new Hashtable<Long, String>(10);    // [Long nexthop] = String loopback

            VariableBinding[] vbs = routeResPdu.toArray();
            System.err.println("Size of VariableBinding: " + vbs.length);
            // Iterating results
            for(int i=0; i<vbs.length; i++) {
                VariableBinding oneVb = routeResPdu.get(i);
                // (st|l)Link contains the next hop for this reply
                String stNexthop = oneVb.getVariable().toString();
                Long lNexthop = new Long(IPUtils.ipToLong(stNexthop));
                System.err.println(i+": link/"+lNexthop+"/"+stNexthop);

                if (! notTargeted.containsKey(lNexthop)) {
                    // This link is not known = candidate to new adjacency
                    OID oid = oneVb.getOid();
                    // obtain LDP peer ID (loopback)
                    String ldpPeer = IPUtils.intArrayToIp(oid.getValue(), indexOIDRfc1213IpRouteNextHop);

                    // Query Metric1
                    int metric1 = 49152;
                    ResponseEvent re = get(prefixOIDRfc1213IpRouteMetric1 + ldpPeer);
                    if (re != null) {
                        PDU pd = re.getResponse();
                        metric1 = pd.get(0).getVariable().toInt();
                    } else {
                        System.err.println("WARNING: RFC1213:Metric1 returned null");
                    }
                    
                    if (!candidateMetric.containsKey(lNexthop) // If it does not exist yet
                            ||
                        metric1 < ((Integer)candidateMetric.get(lNexthop)).intValue()) {   // or if its metric is lower

                        System.err.println("Targeted adjacency "+ ldpPeer +
                                "(link/"+lNexthop+"/"+IPUtils.longToIp(lNexthop)+") candidate to LDP peer, metric("+metric1+")");

                        candidateMetric.put(lNexthop, new Integer(metric1));
                        candidateLoopback.put(lNexthop, ldpPeer);
                    }
                }
            }

            /**
             * At this point we have candidateLoopback containing reportedly
             * targeted LDP peers (on interfaces with no other LDP peers),
             * that happen to have the shortest metric, so we can conclude 
             * they belong to adjacent devices.
             */

            // This adjacency is reported as targeted, so getting ifNumber via
            // MplsLdpEntityConfGenIfIndexOrZero will return 0 no matter what.

            en = candidateLoopback.keys(); // Iterating thru link addresses
            while(en.hasMoreElements()) {
                Long key = (Long)en.nextElement();
                String ldpPeer = (String)candidateLoopback.get(key);

                int ifIndex = 0;
                ResponseEvent re = get(prefixOIDRfc1213IpRouteIfIndex + ldpPeer);
                if (re != null) {
                    PDU pd = re.getResponse();
                    ifIndex = pd.get(0).getVariable().toInt();
                } else {
                    System.err.println("WARNING: RFC1213:ipRouteIfIndex returned null");
                }

                // and add to hashes
                linkToLoopback.put(key, ldpPeer); // XXX Is this needed?
                notTargeted.put(key, new Integer(ifIndex));
                System.err.println("Promoted:"+ldpPeer);
            }

        } catch(IOException ex) {
            // If we don't have this data, the query for this node fails
            System.err.println("getAdjacenciesCisco: Error while getting ipRouteNextHop");
        }


        // 4 - Translate link to loopback, for results
        System.err.println("STEP4: Results");
        Hashtable<String, Adjacency> adjacencyHash = new Hashtable<String, Adjacency>(10);
        
        en = linkToLoopback.keys();
        while(en.hasMoreElements()) {
            Long link = (Long)en.nextElement();
            String adjAddress = (String)linkToLoopback.get(link);
            Adjacency remote = new Adjacency(adjAddress);
            if (notTargeted.containsKey(link)) {
                int ifIndex = ((Integer)notTargeted.get(link)).intValue();
                remote.setSNMPInterface(ifIndex);

                // We have ifIndex, so let's add it as valid adjacency.
                // XXX might not be a valid assumption
                System.err.println("Adjacency: "+adjAddress+", remote:"+remote);
                adjacencyHash.put(adjAddress, remote);  // Adding adjacency
            }
        }
        return adjacencyHash;
    }

    /**
     * Version Juniper to obtain adjacencies
     *
     * First piece of code is similar (except for OID). The SNMP interface
     * information has to be retrieved from a second query.
     *
     * The list of adjacent nodes is obtained by querying the SNMP variable
     * jnxMplsLdpHelloAdjType (Juniper) or mplsLdpHelloAdjacencyType (Cisco).
     * This query returns the LDP Hello adjacencies, and the function takes
     * the ones that have been seen via 'Link' (value 1).
     *
     * On the Juniper response OID, it is encoded the SNMP ifIndex of the
     * interface where the adjacency has been established.
     */
    private Hashtable<String, Adjacency> getAdjacenciesJuniper() {
        final int JNX_ADJ_IP_INDEX = 7;     // First IP address octect for field on TableEvent
        final int JNX_SNMP_IF_INDEX = 13;   // SNMP ifNumber index for field on TableEvent
        // LinkedList of "Adjacent" objects
        Hashtable<String, Adjacency> adjacencyHash = null;

        System.err.println("Retrieving adjacencies for node "+target.getAddress());

        // Retrieve LDP adjacency list
        argOID[0] = jnxMplsLdpHelloAdjTypeOID;
        List<?> en = table.getTable(target, argOID, OIDLowerBound, OIDUpperBound);
        if (((RetrievalEvent)en.get(0)).getStatus() != RetrievalEvent.STATUS_OK) {
            // Timeout
            System.err.println("getTable: SNMP query timed out.");
            return null;
        }

        // Here are the results. We are only interested in the
        // adjacencies found on links(1), not targeted(2)
        ListIterator<?> it = en.listIterator();
        int [] oid_index = null;
        int adjType;
        String adjAddress;
        adjacencyHash = new Hashtable<String, Adjacency>(); // XXX initialCapacity
        while( it.hasNext()) {
            TableEvent ev = (TableEvent)it.next();
            // juniper: node queried -> AA.BB.CC.DD
            // org.snmp4j.util.TableEvent[
            //       THIS NODE IP          ADJACENCY IP        SNMP IF
            // index=(AA.BB.CC.DD).0.0.1.(EE.FF.GG.HH).0.0.(16),
            // vbs=[1.3.6.1.4.1.2636.3.36.1.3.5.1.1.3.AA.BB.CC.DD.0.0.1.EE.FF.GG.HH.0.0.16 = 2],
            // status=0,exception=null,report=null]
            // index, integers from 7 to 10 are the peer
            // value of vbs shows adjacency type
            oid_index = ev.getIndex().getValue();
            // This is the value for the OID
            adjType = ev.getColumns()[0].getVariable().toInt();
            
            if (adjType == 1) { // link-type adjacency
                // Build string literal with adjacency IP address
                adjAddress = IPUtils.intArrayToIp(oid_index, JNX_ADJ_IP_INDEX);

                Adjacency remote = new Adjacency(adjAddress);
                // Set SNMP local interface number (ifNumber)
                remote.setSNMPInterface(oid_index[JNX_SNMP_IF_INDEX]);
                adjacencyHash.put(adjAddress, remote);

                System.err.println("Adjacency: "+adjAddress+", remote:"+remote);
            }
        }        
        return adjacencyHash;
    }
    
    /**
     * Retrieves a list of LDP Hello adjacencies, as well as the local
     * interface on which the router 'sees' them.
     * 
     * @return A Map interface to found adjacencies (key=String with IP
     * address, value=Adjacency object), null if error
     *
     * TODO: add checks for non-initialized input data
     */
    public Hashtable<String, Adjacency> getAdjacencies() {

        Hashtable<String, Adjacency> result = null;

        if (unknownArch) {
            System.err.println("SNMPQuery:getAdjacencies: Unknown architecture.");
            return null;
        }

        if (isCisco) {
            result = getAdjacenciesCisco();
        } else {
            result = getAdjacenciesJuniper();
        }
     
        // Retrieve interface names (ifDescr)
        getInterfaces(result);

        return result;
    }

    /**
     * Traverses the adjacency hashtable and finds interface names
     * taking ifNumber and looking up ifDescr. Interfaces speed
     * (by querying ifHighSpeed) is also retrieved.
     *
     * @return true if operation was successful
     */
    private boolean getInterfaces(final Hashtable<String, Adjacency> adjs) {
        /*
         * The idea is generate a query for the descriptions and speed of
         * the interfaces, and then assign them to the appropriate adjacency.
         *
         * The Hashtable object we receive is indexed by the IP address, so
         * we may want to have a temporary hashtable indexed by ifNumber.
         */

        class IndexHash {
            /**
             * This hash should have whatever we need as key, and a Set
             * of Objects for values.
             */
            Hashtable<String, ArrayList<Object>> indexHash;
            IndexHash(int initialSize) {
                indexHash = new Hashtable<String, ArrayList<Object>>(initialSize);
            }
            void put(String key, Object value) {
                ArrayList<Object> lst;
                if (indexHash.containsKey(key)) {
                    lst = (ArrayList<Object>)indexHash.get(key);
                    lst.add(value);
                } else {
                    lst = new ArrayList<Object>();
                    lst.add(value);
                    indexHash.put(key, lst);
                }
            }

            Iterator<?> get(Object key) {
                ArrayList<?> lst = (ArrayList<?>)indexHash.get(key);
                return lst.iterator();
            }
        }

        PDU pdu = new PDU();
        Adjacency one;
        IndexHash ifIndexHash = new IndexHash(adjs.size());

        Enumeration<Adjacency> en = adjs.elements();
        while(en.hasMoreElements()) {
            one = (Adjacency)en.nextElement();
            // Add OIDs
            pdu.add(new VariableBinding(new OID(prefixOIDIfDescr + one.getSNMPInterface())));
            pdu.add(new VariableBinding(new OID(prefixOIDIfHighSpeed + one.getSNMPInterface())));
            // TODO - Need to convert this hash into a class, and hide
            // its details from here. Consider the case of several
            // reported LDP neighbours over the same interface
            ifIndexHash.put(String.valueOf(one.getSNMPInterface()), one);
        }

        try {
            ResponseEvent rev = snmp.get(pdu, target);
            PDU respdu = rev.getResponse();
            if (respdu == null) {
                System.err.println("getInterfaceNames: snmp timeout");
                return false;
            }
            VariableBinding[] vbs = respdu.toArray();
            for(int i=0; i<vbs.length; i+=2) {
                // Find from the OID what IP this value is for
                OID oid = respdu.get(i).getOid();
                // Get IP Address
                String ifIndex = String.valueOf(oid.getValue()[indexOIDIfDescr]);
                // Get interface name ...
                String ifDescr = respdu.get(i).getVariable().toString();
                //System.out.println("ifIndex:"+ifIndex+", ifDescr:"+ifDescr);
                // ... its speed (units is Mb/s) ...
                // XXX What happens if a device has no ifHighSpeed? results
                // here will slip and this code will break
                long ifHighSpeed = respdu.get(i+1).getVariable().toLong();
                /*System.out.println("ifIndex:"+ifIndex+", ifDescr:"+ifDescr+", ifHighSpeed:"+ifHighSpeed);*/

                // ... retrieve adjacency or adjacencies...
                Iterator<?> it = ifIndexHash.get(ifIndex);
                while(it.hasNext()) {
                    Adjacency remote = (Adjacency)it.next();
//                    Adjacency remote = (Adjacency)ifIndexHash.get(ifIndex);
                    // ... and lst correct value
                    remote.setRouterInterface(ifDescr, ifHighSpeed);
                }
            }
        } catch(IOException ex) {
            System.err.println("getInterfaceNames: IOException");
            return false;
        }
        return true;
    }

    public String toString() {
        return "SnmpQuery ("+ s_address + ", " + s_community + ")";
    }
    
}

package uk.co.gockett.ipbtools.topology;

/**
 * Holds adjacency data for an adjacency
 * @author ruanoj@github
 */
public class Adjacency {
    private String remoteAddress;
    private String localInterface = null;
    private long localSpeed = 0L;
    private int snmpLocalInterface = 0;

    public Adjacency(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setRouterInterface(String localInterface, long localSpeed) {
        this.localInterface = localInterface;
        this.localSpeed = localSpeed;
    }

    public String getRouterInterface() {
        return localInterface;
    }

    public long getSpeed() {
        return localSpeed;
    }
    
    public void setSNMPInterface(int ifNumber) {
        this.snmpLocalInterface = ifNumber;
    }

    public int getSNMPInterface() {
        return snmpLocalInterface;
    }

    public String getAdjacentAddress() {
        return remoteAddress;
    }
    public String toString() {
        return  "Adjacency[remoteAddress:"+remoteAddress+
                ", localInterface:"+localInterface+
                ", snmpLocalInterface:"+snmpLocalInterface+"]";
    }
}

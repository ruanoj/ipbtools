package uk.co.gockett.ipbtools.topology;

/**
 * Interface meant to receive status updates from arbitrary objects
 * @author ruanoj@github
 */
public interface DiscoveryListener {

    void join();

    void leave();

    void working();

    void successResult();

    void errorResult();

}

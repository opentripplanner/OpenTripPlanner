package org.opentripplanner.util.monitoring;


/** 
 * A singleton factory for getting a monitoring store.
 * @author novalis
 *
 */
public class MonitoringStoreFactory {

    private static MonitoringStore store;

    public static synchronized MonitoringStore getStore() {
        if (store == null) {
            store = new MonitoringStore();
        }
        return store;
    }
}

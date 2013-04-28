package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.RoutingRequest;

/**
 * Default implementation of ShortestPathTreeFactory.
 * 
 * Creates a MultiShortestPathTree for any transit or bike/walk trips, otherwise uses
 * BasicShortestPathTree.
 * 
 * @author avi
 */
public class DefaultShortestPathTreeFactory implements ShortestPathTreeFactory {

    @Override
    public ShortestPathTree create(RoutingRequest options) {
        ShortestPathTree spt = null;
        if (options.getModes().isTransit() || options.getModes().getWalk()
                && options.getModes().getBicycle()) {
            spt = new MultiShortestPathTree(options);
        } else {
            spt = new BasicShortestPathTree(options);
        }
        return spt;
    }

}

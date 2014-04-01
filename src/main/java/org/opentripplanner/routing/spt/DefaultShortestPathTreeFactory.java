package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.core.RoutingRequest;

/**
 * Default implementation of ShortestPathTreeFactory.
 * 
 * Creates a MultiShortestPathTree for any transit, bike/walk (Bike rental) or car/walk (P+R) trips,
 * otherwise uses BasicShortestPathTree.
 * 
 * @author avi
 */
public class DefaultShortestPathTreeFactory implements ShortestPathTreeFactory {

    @Override
    public ShortestPathTree create(RoutingRequest options) {
        ShortestPathTree spt = null;
        if (options.getModes().isTransit() || options.getModes().getWalk()
                && options.getModes().getBicycle() || options.getModes().getWalk()
                && options.getModes().getCar()) {
            spt = new MultiShortestPathTree(options);
        } else {
            spt = new BasicShortestPathTree(options);
        }
        return spt;
    }

}

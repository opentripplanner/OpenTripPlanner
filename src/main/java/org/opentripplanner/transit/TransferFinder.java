package org.opentripplanner.transit;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.streets.StreetLayer;
import org.opentripplanner.streets.StreetRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TransferFinder {

    private static Logger LOG = LoggerFactory.getLogger(TransferFinder.class);

    TransitLayer transitLayer;

    StreetLayer streetLayer;

    StreetRouter streetRouter;

    /**
     * Should chooses whether to search via the street network or straight line distance based on the presence of
     * OSM street data (whether the street layer is null). However the street layer will always contain transit
     * stop vertices so not sure that can work.
     */
    public TransferFinder(TransitLayer transitLayer, StreetLayer streetLayer, int radiusMeters) {
        this.transitLayer = transitLayer;
        this.streetLayer = streetLayer;
        streetRouter = new StreetRouter(streetLayer);
        streetRouter.distanceLimitMeters = radiusMeters;
    }

    public void findTransfers () {

        LOG.info("Finding transfers through the street network from all stops...");
        final TIntArrayList EMPTY_INT_LIST = new TIntArrayList(); // Optimization: use the same empty list for all stops with no transfers

        // For each stop, all transfers out of that stop packed as pairs of (toStopIndex, distance)
        List<TIntList> transfersForStop = new ArrayList<>(transitLayer.getStopCount());
        for (int s = 0; s < transitLayer.getStopCount(); s++) {
            // From each stop, run a street search looking for other transit stops.
            int originStreetVertex = transitLayer.streetVertexForStop.get(s);
            if (originStreetVertex == -1) {
                LOG.warn("Stop {} is not connected to the street network.", s);
                // Every iteration must add an array to transfersForStop to maintain the right length.
                transfersForStop.add(EMPTY_INT_LIST);
                continue;
            }
            streetRouter.setOrigin(originStreetVertex);
            streetRouter.route();
            TIntIntMap timesToReachedStops = streetRouter.getReachedStops();
            // Filter down the list of target stops to only include those stops that are the closest on some pattern.
            TIntIntMap bestStopOnPattern = new TIntIntHashMap(50, 0.5f, -1, -1);
            // For every reached stop,
            timesToReachedStops.forEachEntry((stopIndex, timeToStop) -> {
                // For every pattern passing through that stop,
                transitLayer.patternsForStop.get(stopIndex).forEach(patternIndex -> {
                    int currentBestStop = bestStopOnPattern.get(patternIndex);
                    // Record this stop if it's the closest one yet seen on that pattern.
                    if (currentBestStop == -1) {
                        bestStopOnPattern.put(patternIndex, stopIndex);
                    } else {
                        int currentBestTime = timesToReachedStops.get(currentBestStop);
                        if (currentBestTime > timeToStop) {
                            bestStopOnPattern.put(patternIndex, stopIndex);
                        }
                    }
                    return true; // iteration should continue
                });
                return true; // iteration should continue
            });

            // At this point we have the indexes of all stops that are the closest one on some pattern.
            // Make transfers to them.
            TIntSet usefulTargetStops = new TIntHashSet();
            usefulTargetStops.addAll(bestStopOnPattern.valueCollection());
            // Pack transfers as pairs of (target stop index, distance)
            TIntList packedTransfers = new TIntArrayList();
            // LOG.info("From {}", transitLayer.stopForIndex.get(s).stop_code);
            usefulTargetStops.forEach(targetStopIndex -> {
                packedTransfers.add(targetStopIndex);
                packedTransfers.add(timesToReachedStops.get(targetStopIndex));
                // LOG.info("{} at {}m", transitLayer.stopForIndex.get(targetStopIndex).stop_code, timegetReachedStops(targetStopIndex));
                return true;
            });
            // Record this list of transfers as coming out of the stop with index s.
            if (packedTransfers.size() > 0) {
                transfersForStop.add(packedTransfers);
            } else {
                transfersForStop.add(EMPTY_INT_LIST);
            }
        }
        // Store the transfers in the transit layer
        transitLayer.transfersForStop = transfersForStop;
        LOG.info("Done finding transfers.");
    }


    /**
     * Return all stops within a certain radius of the given vertex, using straight-line distance independent of streets.
     * If the origin vertex is a TransitStop, the result will include it.
     */
    /* Skip stops that are entrances to stations or whose entrances are coded separately */
    /* Determine the set of stops that are already reachable via other pathways or transfers */

}

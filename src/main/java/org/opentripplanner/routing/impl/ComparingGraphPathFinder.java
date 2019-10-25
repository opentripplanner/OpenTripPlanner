package org.opentripplanner.routing.impl;

import com.google.common.collect.Lists;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This variant of the GraphPathFinder "fans out" a routing request into potentially multiple ones and compares the
 * sets of results. It can then discard nonsensical or inferiour results.
 *
 * This is useful for the mode PARK_RIDE because this mode forces you to drive to a P&R station and take public transport
 * even if it would be _a lot_ faster to just drive to the destination all the way.
 */
public class ComparingGraphPathFinder extends GraphPathFinder {

    private static final Logger LOG = LoggerFactory.getLogger(ComparingGraphPathFinder.class);

    public ComparingGraphPathFinder(Router router) {
        super(router);
    }

    @Override
    public List<GraphPath> graphPathFinderEntryPoint(RoutingRequest options) {

        List<GraphPath> results;
        if (options.parkAndRide) {
            // TODO: need to find out how to run this in parallel

            LOG.info("Detected a P&R routing request. Will execute two requests to also get car-only routes.");

            // first the normal P&R
            List<GraphPath> parkAndRideResults = new GraphPathFinder(router).graphPathFinderEntryPoint(options);

            // now car-only
            RoutingRequest carOnlyOptions = options.clone();
            carOnlyOptions.parkAndRide = false;
            carOnlyOptions.setModes(new TraverseModeSet(TraverseMode.CAR));

            assert(carOnlyOptions.modes.getCar());
            List<GraphPath> carOnlyResult = new GraphPathFinder(router).graphPathFinderEntryPoint(carOnlyOptions);

            results = Lists.newLinkedList();
            results.addAll(parkAndRideResults);
            results.addAll(carOnlyResult);
        } else {
            results = super.graphPathFinderEntryPoint(options);
        }

        return results;
    }
}

package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This variant of the GraphPathFinder "fans out" a routing request into potentially multiple ones and compares the
 * sets of results. It can then discard nonsensical or inferiour results.
 * <p>
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
            LOG.info("Detected a P&R routing request. Will execute two requests to also get car-only routes.");

            // the normal P&R
            CompletableFuture<List<GraphPath>> parkAndRideResults = CompletableFuture.supplyAsync(() -> new GraphPathFinder(router).graphPathFinderEntryPoint(options));
            // car-only
            CompletableFuture<List<GraphPath>> carOnlyResult = CompletableFuture.supplyAsync(() -> runCarOnlyRequest(options));
            // the CompletableFutures are there to make sure that the computations run in parallel
            results = Stream.of(parkAndRideResults, carOnlyResult)
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());


        } else {
            results = super.graphPathFinderEntryPoint(options);
        }

        return results;
    }

    private List<GraphPath> runCarOnlyRequest(RoutingRequest originalOptions) {
        RoutingRequest carOnlyOptions = originalOptions.clone();
        carOnlyOptions.parkAndRide = false;
        carOnlyOptions.setModes(new TraverseModeSet(TraverseMode.CAR));

        return new GraphPathFinder(router).graphPathFinderEntryPoint(carOnlyOptions);
    }
}

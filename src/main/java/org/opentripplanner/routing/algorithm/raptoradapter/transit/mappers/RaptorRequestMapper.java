package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.util.OTPFeature;

public class RaptorRequestMapper {
    private final RoutingRequest request;
    private final Collection<? extends RaptorTransfer> accessPaths;
    private final Collection<? extends RaptorTransfer> egressPaths;
    private final long transitSearchTimeZeroEpocSecond;

    private RaptorRequestMapper(
            RoutingRequest request,
            Collection<? extends RaptorTransfer> accessPaths,
            Collection<? extends RaptorTransfer> egressPaths,
            long transitSearchTimeZeroEpocSecond
    ) {
        this.request = request;
        this.accessPaths = accessPaths;
        this.egressPaths = egressPaths;
        this.transitSearchTimeZeroEpocSecond = transitSearchTimeZeroEpocSecond;
    }

    public static RaptorRequest<TripSchedule> mapRequest(
            RoutingRequest request,
            ZonedDateTime transitSearchTimeZero,
            Collection<? extends RaptorTransfer> accessPaths,
            Collection<? extends RaptorTransfer> egressPaths
    ) {
        return new RaptorRequestMapper(
                request,
                accessPaths,
                egressPaths,
                transitSearchTimeZero.toEpochSecond()
        ).doMap();
    }

    private RaptorRequest<TripSchedule> doMap(
    ) {
        var builder = new RaptorRequestBuilder<TripSchedule>();
        var searchParams = builder.searchParams();

        if(request.pageCursor ==  null) {
            int time = relativeTime(request.getDateTime());
            if (request.arriveBy) {
                searchParams.latestArrivalTime(time);
            }
            else {
                searchParams.earliestDepartureTime(time);
            }
            searchParams.searchWindow(request.searchWindow);
        }
        else {
            var c = request.pageCursor;

            if (c.earliestDepartureTime != null) {
                searchParams.earliestDepartureTime(relativeTime(c.earliestDepartureTime));
            }
            if (c.latestArrivalTime != null) {
                searchParams.latestArrivalTime(relativeTime(c.latestArrivalTime));
            }
            searchParams.searchWindow(c.searchWindow);
        }

        if(request.maxTransfers != null) {
            searchParams.maxNumberOfTransfers(request.maxTransfers);
        }

        builder
                .profile(RaptorProfile.MULTI_CRITERIA)
                .enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION)
                .slackProvider(new SlackProvider(
                        request.transferSlack,
                        request.boardSlack,
                        request.boardSlackForMode,
                        request.alightSlack,
                        request.alightSlackForMode
                ));

        builder
                .searchParams()
                .timetableEnabled(request.timetableView)
                .constrainedTransfersEnabled(OTPFeature.TransferConstraints.isOn())
                .addAccessPaths(accessPaths)
                .addEgressPaths(egressPaths);

        if(request.raptorDebuging != null) {
            var debug = builder.debug();
            var debugLogger = new SystemErrDebugLogger(true);

            debug.addStops(request.raptorDebuging.stops())
                    .setPath(request.raptorDebuging.path())
                    .debugPathFromStopIndex(request.raptorDebuging.debugPathFromStopIndex())
                    .stopArrivalListener(debugLogger::stopArrivalLister)
                    .patternRideDebugListener(debugLogger::patternRideLister)
                    .pathFilteringListener(debugLogger::pathFilteringListener)
                    .logger(debugLogger);
        }

        if(!request.timetableView && request.arriveBy) {
            builder.searchParams().preferLateArrival(true);
        }

        return builder.build();
    }

    private int relativeTime(Instant time) {
        return (int)(time.getEpochSecond() - transitSearchTimeZeroEpocSecond);
    }
}

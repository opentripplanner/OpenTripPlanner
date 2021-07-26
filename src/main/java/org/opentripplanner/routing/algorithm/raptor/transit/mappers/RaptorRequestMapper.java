package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import java.time.ZonedDateTime;
import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptor.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.util.OTPFeature;

public class RaptorRequestMapper {

    public static RaptorRequest<TripSchedule> mapRequest(
            RoutingRequest request,
            ZonedDateTime startOfTime,
            Collection<? extends RaptorTransfer> accessPaths,
            Collection<? extends RaptorTransfer> egressPaths
    ) {
        RaptorRequestBuilder<TripSchedule> builder = new RaptorRequestBuilder<>();

        int time = DateMapper.secondsSinceStartOfTime(
                startOfTime,
                request.getDateTime().toInstant()
        );

        if (request.arriveBy) {
            builder.searchParams().latestArrivalTime(time);
        }
        else {
            builder.searchParams().earliestDepartureTime(time);
        }
        if(request.maxTransfers != null) {
            builder.searchParams().maxNumberOfTransfers(request.maxTransfers);
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
                .searchWindow(request.searchWindow)
                .timetableEnabled(request.timetableView)
                .guaranteedTransfersEnabled(OTPFeature.GuaranteedTransfers.isOn())
                .addAccessPaths(accessPaths)
                .addEgressPaths(egressPaths);

        if(!request.timetableView && request.arriveBy) {
            builder.searchParams().preferLateArrival(true);
        }

        return builder.build();
    }
}

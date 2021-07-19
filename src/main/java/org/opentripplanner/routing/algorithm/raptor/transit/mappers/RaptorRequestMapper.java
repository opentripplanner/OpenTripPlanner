package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.raptor.api.request.McCostParams;
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
                .forbiddenTransfersEnabled(OTPFeature.ForbiddenTransfers.isOn())
                .addAccessPaths(accessPaths)
                .addEgressPaths(egressPaths);

        if(!request.timetableView && request.arriveBy) {
            builder.searchParams().preferLateArrival(true);
        }

        builder.mcCostFactors()
                .transferCost(request.transferCost)
                .waitReluctanceFactor(request.waitReluctance);

        if (request.modes.transferMode == StreetMode.BIKE) {
            builder.mcCostFactors()
                    .boardCost(request.bikeBoardCost);
        }
        else {
            builder.mcCostFactors()
                    .boardCost(request.walkBoardCost);
        }
        builder.mcCostFactors().transitReluctanceFactors(
            mapTransitReluctance(request.transitReluctanceForMode())
        );

        return builder.build();
    }

    public static double[] mapTransitReluctance(Map<TransitMode, Double> map) {

        if(map.isEmpty()) { return null; }

        // The transit reluctance is arranged in an array with the {@link TransitMode} ordinal
        // as an index. This make the lookup very fast and the size of the array small.
        // We could get away with a smaller array if we kept an index from mode to index
        // and passed that into the transit layer and used it to set the
        // {@link TripScheduleWithOffset#transitReluctanceIndex}, but this is difficult with the
        // current transit model design.
        double[] transitReluctance = new double[TransitMode.values().length];
        Arrays.fill(transitReluctance, McCostParams.DEFAULT_TRANSIT_RELUCTANCE);
        for (TransitMode mode : map.keySet()) {
                transitReluctance[mode.ordinal()] = map.get(mode);
        }
        return transitReluctance;
    }
}

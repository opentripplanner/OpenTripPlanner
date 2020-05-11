package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch;


/**
 * Build a path from a destination arrival - this maps between the domain of routing
 * to the domain of result paths. All values not needed for routing is computed as part of this mapping.
 * <p/>
 * This mapper maps the result of a reverse-search. And is therefor responsible for doing some adjustments
 * to the result. The internal state stores 'latest possible arrivaltimes', so to map back to paths the
 * 'board slack' is removed from the 'latest possible arrivaltime' to get next legs 'boardTime'. Also,
 * the path is reversed again, so the original origin - temporally made destination - is returned to origin
 * again ;-)
 * <p/>
 * This mapper uses recursion to reverse the results.
 */
public final class ReversePathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {

    private int iterationDepartureTime = -1;

    /**
     * Note! This is not the Raptor internal SlackProvider, but the slack-provider
     * submitted by the transit layer.
     */
    private final RaptorSlackProvider transitLayerSlackProvider;

    public ReversePathMapper(RaptorSlackProvider slackProvider, WorkerLifeCycle lifeCycle) {
        this.transitLayerSlackProvider = slackProvider;
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
        ArrivalView<T> lastStopArrival;
        AccessPathLeg<T> accessLeg;

        // The path is in reverse
        lastStopArrival = destinationArrival.previous();

        accessLeg = mapAccessLeg(destinationArrival, lastStopArrival);

        return new Path<>(
                iterationDepartureTime,
                accessLeg,
                RaptorCostConverter.toOtpDomainCost(destinationArrival.cost())
        );
    }

    AccessPathLeg<T> mapAccessLeg(
            DestinationArrival<T> destinationArrival,
            ArrivalView<T> lastStopArrival
    ) {
        return new AccessPathLeg<>(
                destinationArrival.accessEgress(),
                lastStopArrival.stop(),
                destinationArrival.arrivalTime(),
                destinationArrival.departureTime(),
                mapToTransit(lastStopArrival)   // Recursive
        );
    }

    private TransitPathLeg<T> mapToTransit(ArrivalView<T> fromStopArrival) {
        // In reverse the previous is in our "toStop"
        ArrivalView<T> toStopArrival = fromStopArrival.previous();
        T tripSchedule = fromStopArrival.trip();

        // Map stops and times into a forward search context
        int fromStop = fromStopArrival.stop();
        int toStop = toStopArrival.stop();
        int arrivalTime = toStopArrival.departureTime();

        TripTimesSearch.Result r = TripTimesSearch.searchBeforeLAT(
                tripSchedule,
                fromStop,
                toStop,
                arrivalTime
        );

        // Recursive call to map next leg
        PathLeg<T> next = mapNextLeg(toStopArrival, tripSchedule, r.alightTime);

        return new TransitPathLeg<>(
                fromStop, r.boardTime, toStop, r.alightTime, tripSchedule, next
        );
    }

    private PathLeg<T> mapNextLeg(
            ArrivalView<T> fromStopArrival,
            T tripSchedule,
            int prevStopArrivalTime
    ) {
        if(fromStopArrival.arrivedByTransit()) {
            return mapToTransit(fromStopArrival);
        }
        else if(fromStopArrival.arrivedByTransfer()) {
            return mapToTransfer(fromStopArrival);
        }
        else {
            return mapToEgressLeg(fromStopArrival, tripSchedule, prevStopArrivalTime);
        }
    }

    private TransferPathLeg<T> mapToTransfer(ArrivalView<T> fromStopArrival) {
        ArrivalView<T> toStopArrival = fromStopArrival.previous();

        return new TransferPathLeg<T>(
                fromStopArrival.stop(),
                fromStopArrival.arrivalTime(),
                toStopArrival.stop(),
                fromStopArrival.departureTime(),
                mapToTransit(toStopArrival)
        );
    }

    private EgressPathLeg<T> mapToEgressLeg(
            ArrivalView<T> stopArrival,
            T tripSchedule,
            int transitAlightTime
    ) {
        // We are searching in reverse so we need to swap `alightSlack` and `boardSlack`
        int boardSlack = transitLayerSlackProvider.alightSlack(tripSchedule.pattern());
        int fromTime = transitAlightTime + boardSlack;
        int toTime = fromTime + (stopArrival.departureTime() - stopArrival.arrivalTime());
        return new EgressPathLeg<>(stopArrival.accessEgress(), stopArrival.stop(), fromTime, toTime);
    }
}

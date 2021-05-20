package org.opentripplanner.transit.raptor.rangeraptor.path;

import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearch;

import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.transit.raptor.rangeraptor.transit.BoarAndAlightTime;


/**
 * Build a path from a destination arrival - this maps between the domain of routing to the domain
 * of result paths. All values not needed for routing is computed as part of this mapping.
 * <p/>
 * This mapper maps the result of a reverse-search. And is therefore responsible for doing some
 * adjustments to the result. The internal state stores 'latest possible arrival-times', so to map
 * back to paths the 'board slack' is removed from the 'latest possible arrival-time' to get the
 * next legs 'boardTime'. Also, the path is reversed again, so the original origin - temporally made
 * destination - is returned to origin again ;-)
 * <p/>
 * This mapper uses recursion to reverse the results.
 */
public final class ReversePathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {

    private final RaptorSlackProvider slackProvider;
    private int iterationDepartureTime = -1;

    public ReversePathMapper(RaptorSlackProvider slackProvider, WorkerLifeCycle lifeCycle) {
        this.slackProvider = slackProvider;
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {

        AccessPathLeg<T> accessLeg = mapAccessLeg(destinationArrival);

        return new Path<>(
                iterationDepartureTime,
                accessLeg,
                RaptorCostConverter.toOtpDomainCost(destinationArrival.cost())
        );
    }

    AccessPathLeg<T> mapAccessLeg(DestinationArrival<T> destArrival) {
        ArrivalView<T> prevArrival = destArrival.previous();
        RaptorTransfer access = destArrival.egressPath().egress();
        int departureTime = destArrival.arrivalTime();

        // Pass arrival time on to next leg (if access is flex the next leg could be a transfer)
        int arrivalTime = destArrival.arrivalTime() + access.durationInSeconds();

        return new AccessPathLeg<>(
                access,
                prevArrival.stop(),
                departureTime,
                arrivalTime,
                domainCost(destArrival),
                mapNextLeg(prevArrival, arrivalTime)
        );
    }

    private PathLeg<T> mapNextLeg(ArrivalView<T> fromStopArrival, int prevStopArrivalTime) {
        if(fromStopArrival.arrivedByTransit()) {
            return mapToTransit(fromStopArrival);
        }
        else if(fromStopArrival.arrivedByTransfer()) {
            return mapToTransfer(fromStopArrival, prevStopArrivalTime);
        }
        else if(fromStopArrival.arrivedByAccess()) {
            return mapToEgressLeg(fromStopArrival, prevStopArrivalTime);
        }
        throw new IllegalStateException("Unknown path type for: " + fromStopArrival);
    }

    private TransitPathLeg<T> mapToTransit(ArrivalView<T> fromStopArrival) {
        // In reverse the previous is in our "toStop"
        ArrivalView<T> toStopArrival = fromStopArrival.previous();

        // Map stops and times into a forward search context
        int fromStop = fromStopArrival.stop();
        int toStop = toStopArrival.stop();

        BoarAndAlightTime r = findTripReverseSearch(fromStopArrival);
        T trip = fromStopArrival.transitPath().trip();
        int arrivalTime = r.alightTime() + slackProvider.alightSlack(trip.pattern());

        return new TransitPathLeg<>(
            fromStop,
            r.boardTime(),
            toStop,
            r.alightTime(),
            domainCost(fromStopArrival),
            trip,
            // Recursive call to map next leg
            mapNextLeg(toStopArrival, arrivalTime)
        );
    }

    private TransferPathLeg<T> mapToTransfer(ArrivalView<T> fromStopArrival, int prevStopArrivalTime) {
        ArrivalView<T> toStopArrival = fromStopArrival.previous();

        // time-shift transit to start immediate after previous leg stop-arrival-time
        // including alight-slack
        int arrivalTime = prevStopArrivalTime + fromStopArrival.transferPath().durationInSeconds();

        return new TransferPathLeg<>(
                fromStopArrival.stop(),
                prevStopArrivalTime,
                toStopArrival.stop(),
                arrivalTime,
                domainCost(fromStopArrival),
                fromStopArrival.transferPath().transfer(),
                mapNextLeg(toStopArrival, arrivalTime)
        );
    }

    private EgressPathLeg<T> mapToEgressLeg(ArrivalView<T> accessArrival, int prevStopArrivalTime) {
        RaptorTransfer egress = accessArrival.accessPath().access();
        int targetFromTime = prevStopArrivalTime;

        if(egress.hasRides()) {
            targetFromTime += slackProvider.transferSlack();
        }

        targetFromTime = egress.earliestDepartureTime(targetFromTime);

        int targetToTime = targetFromTime + egress.durationInSeconds();

        // No need to time-shift the egress leg, this is done when stopArrival is created
        return new EgressPathLeg<>(
            egress,
            accessArrival.stop(),
            targetFromTime,
            targetToTime,
            RaptorCostConverter.toOtpDomainCost(accessArrival.cost())
        );
    }

    private int domainCost(ArrivalView<T> from) {
        if(from.cost() == -1) { return -1; }
        return RaptorCostConverter.toOtpDomainCost(from.cost() - from.previous().cost());
    }
}

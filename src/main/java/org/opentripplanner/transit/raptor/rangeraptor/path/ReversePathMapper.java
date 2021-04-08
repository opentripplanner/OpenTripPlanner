package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.model.base.ToStringBuilder;
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

import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearch;


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

    /** Current/last transit alight time needed to time-shift EgressPathLeg(access arrival)  */
    private int alightTime;

    /** Current/last trip needed to time-shift EgressPathLeg(access arrival)  */
    private T trip;


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
        int targetDepartureTime = destArrival.arrivalTime();
        int targetArrivalTime = destArrival.arrivalTime() + access.durationInSeconds();

        return new AccessPathLeg<>(
                access,
                prevArrival.stop(),
                targetDepartureTime,
                targetArrivalTime,
                domainCost(destArrival),
                mapNextLeg(prevArrival)
        );
    }

    private PathLeg<T> mapNextLeg(ArrivalView<T> fromStopArrival) {
        if(fromStopArrival.arrivedByTransit()) {
            return mapToTransit(fromStopArrival);
        }
        else if(fromStopArrival.arrivedByTransfer()) {
            return mapToTransfer(fromStopArrival);
        }
        else if(fromStopArrival.arrivedByAccess()) {
            return mapToEgressLeg(fromStopArrival);
        }
        throw new IllegalStateException("Unknown path type for: " + fromStopArrival);
    }

    private TransitPathLeg<T> mapToTransit(ArrivalView<T> fromStopArrival) {
        // In reverse the previous is in our "toStop"
        ArrivalView<T> toStopArrival = fromStopArrival.previous();
        trip = fromStopArrival.transitPath().trip();

        // Map stops and times into a forward search context
        int fromStop = fromStopArrival.stop();
        int toStop = toStopArrival.stop();

        BoarAndAlightTime r = findTripReverseSearch(fromStopArrival);

        alightTime = r.alightTime();

        return new TransitPathLeg<>(
            fromStop,
            r.boardTime(),
            toStop,
            r.alightTime(),
            domainCost(fromStopArrival),
            trip,
            // Recursive call to map next leg
            mapNextLeg(toStopArrival)
        );
    }

    private TransferPathLeg<T> mapToTransfer(ArrivalView<T> fromStopArrival) {
        ArrivalView<T> toStopArrival = fromStopArrival.previous();

        int targetArrivalTime = fromStopArrival.arrivalTime() + fromStopArrival
            .transferPath()
            .durationInSeconds();
        return new TransferPathLeg<T>(
                fromStopArrival.stop(),
                fromStopArrival.arrivalTime(),
                toStopArrival.stop(),
                targetArrivalTime,
                domainCost(fromStopArrival),
                fromStopArrival.transferPath().transfer(),
                mapToTransit(toStopArrival)
        );
    }

    private EgressPathLeg<T> mapToEgressLeg(ArrivalView<T> accessArrival) {
        RaptorTransfer egress = accessArrival.accessPath().access();
        int targetFromTime = alightTime + slackProvider.alightSlack(trip.pattern());

        System.out.println(
            ToStringBuilder.of(getClass())
                .addServiceTime("alightTime", alightTime, -1)
                .addDurationSec("alightSlack", slackProvider.alightSlack(trip.pattern()))
                .addServiceTime("targetFromTime", targetFromTime, 1)
                .toString()
        );

        if(egress.hasRides()) {
            targetFromTime += slackProvider.transferSlack();
            targetFromTime = egress.earliestDepartureTime(targetFromTime);
        }

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

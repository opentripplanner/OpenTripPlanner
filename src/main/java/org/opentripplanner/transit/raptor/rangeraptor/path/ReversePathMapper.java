package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
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

    public ReversePathMapper(WorkerLifeCycle lifeCycle) {
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
        RaptorTransfer access = destArrival.egressLeg().egress();
        int targetDepartureTime = destArrival.arrivalTime();
        int targetArrivalTime = destArrival.arrivalTime() + access.durationInSeconds();

        return new AccessPathLeg<>(
                access,
                prevArrival.stop(),
                targetDepartureTime,
                targetArrivalTime,
                domainCost(destArrival),
                mapToTransit(prevArrival)   // Recursive
        );
    }

    private TransitPathLeg<T> mapToTransit(ArrivalView<T> fromStopArrival) {
        // In reverse the previous is in our "toStop"
        ArrivalView<T> toStopArrival = fromStopArrival.previous();
        T trip = fromStopArrival.transitLeg().trip();

        // Map stops and times into a forward search context
        int fromStop = fromStopArrival.stop();
        int toStop = toStopArrival.stop();

        TripTimesSearch.BoarAlightTimes r = TripTimesSearch.findTripReverseSearch(fromStopArrival);

        // Recursive call to map next leg
        PathLeg<T> next = mapNextLeg(toStopArrival);

        return new TransitPathLeg<>(
            fromStop,
            r.boardTime,
            toStop,
            r.alightTime,
            domainCost(fromStopArrival),
            trip,
            next
        );
    }

    private PathLeg<T> mapNextLeg(ArrivalView<T> fromStopArrival) {
        if(fromStopArrival.arrivedByTransit()) {
            return mapToTransit(fromStopArrival);
        }
        else if(fromStopArrival.arrivedByTransfer()) {
            return mapToTransfer(fromStopArrival);
        }
        else if(fromStopArrival.arrivedByAccessLeg()) {
            return mapToEgressLeg(fromStopArrival);
        }
        throw new IllegalStateException("Unknown leg type for: " + fromStopArrival);
    }

    private TransferPathLeg<T> mapToTransfer(ArrivalView<T> fromStopArrival) {
        ArrivalView<T> toStopArrival = fromStopArrival.previous();

        int targetArrivalTime = fromStopArrival.arrivalTime() + fromStopArrival
            .transferLeg()
            .durationInSeconds();
        return new TransferPathLeg<T>(
                fromStopArrival.stop(),
                fromStopArrival.arrivalTime(),
                toStopArrival.stop(),
                targetArrivalTime,
                domainCost(fromStopArrival),
                mapToTransit(toStopArrival)
        );
    }

    private EgressPathLeg<T> mapToEgressLeg(ArrivalView<T> accessArrival) {
        RaptorTransfer egress = accessArrival.accessLeg().access();
        int targetDepartureTime = accessArrival.arrivalTime();
        int targetArrivalTime = accessArrival.arrivalTime() + egress.durationInSeconds();

        // No need to time-shift the egress leg, this is done when stopArrival is created
        return new EgressPathLeg<>(
            egress,
            accessArrival.stop(),
            targetDepartureTime,
            targetArrivalTime,
            RaptorCostConverter.toOtpDomainCost(accessArrival.cost())
        );
    }

    private int domainCost(ArrivalView<T> from) {
        if(from.cost() == -1) { return -1; }
        return RaptorCostConverter.toOtpDomainCost(from.cost() - from.previous().cost());
    }
}

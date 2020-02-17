package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.transit.CostCalculator;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;


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

    private final TransitCalculator calculator;

    public ReversePathMapper(TransitCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> to) {
        ArrivalView<T> from;
        AccessPathLeg<T> accessLeg;

        // The path is in reverse
        from = to.previous();

        accessLeg = new AccessPathLeg<>(
                to.arrivalTime(),
                from.stop(),
                to.departureTime(),
                mapToTransit(from)   // Recursive
        );

        int destinationArrivalTime = egressLeg(accessLeg).toTime();
        int numberOfTransfers = to.numberOfTransfers();

        return new Path<>(
                accessLeg,
                destinationArrivalTime,
                numberOfTransfers,
                CostCalculator.toOtpDomainCost(to.cost())
        );
    }

    private PathLeg<T> egressLeg(AccessPathLeg<T> accessLeg) {
        PathLeg<T> leg = accessLeg;

        while (!leg.isEgressLeg()) {
            leg = leg.nextLeg();
        }
        return leg;
    }

    private PathLeg<T> mapToPathLeg(ArrivalView<T> to, int transitBoardTime) {
        if (to.arrivedByTransfer()) {
            return mapToTransfer(to);
        }
        else if(to.arrivedByTransit()) {
            return mapToTransit(to);
        }
        else {
            return mapToEgressLeg(to, transitBoardTime);
        }
    }

    private TransitPathLeg<T> mapToTransit(ArrivalView<T> to) {
        ArrivalView<T> from = to.previous();

        return new TransitPathLeg<>(
                to.stop(),
                removeBoardSlack(to.arrivalTime()),
                from.stop(),
                to.departureTime(),
                to.trip(),
                mapToPathLeg(from, to.departureTime())
        );
    }

    private int removeBoardSlack(int arrivalTime) {
        return calculator.removeBoardSlack(arrivalTime);
    }

    private TransferPathLeg<T> mapToTransfer(ArrivalView<T> to) {
        ArrivalView<T> from = to.previous();

        return new TransferPathLeg<T>(
                to.stop(),
                to.arrivalTime(),
                from.stop(),
                to.departureTime(),
                mapToTransit(from)
        );
    }

    private EgressPathLeg<T> mapToEgressLeg(ArrivalView<T> to, int transitBoardTime) {
        int accessDurationInSeconds =  to.departureTime() - to.arrivalTime();
        int egressArrivalTime = calculator.originDepartureTime(transitBoardTime, accessDurationInSeconds);
        return new EgressPathLeg<>(to.stop(), transitBoardTime, egressArrivalTime);
    }
}

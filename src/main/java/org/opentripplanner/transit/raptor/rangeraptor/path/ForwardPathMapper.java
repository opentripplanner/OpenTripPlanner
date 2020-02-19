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
 */
public final class ForwardPathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {
    private final TransitCalculator calculator;

    public ForwardPathMapper(TransitCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
        ArrivalView<T> from;
        ArrivalView<T> to;
        PathLeg<T> lastLeg;
        TransitPathLeg<T> transitLeg;
        int numberOfTransits = 0;

        from = destinationArrival.previous();
        lastLeg = new EgressPathLeg<>(
                from.stop(),
                destinationArrival.departureTime(),
                destinationArrival.arrivalTime()
        );

        do {
            to = from;
            from = from.previous();
            ++numberOfTransits;

            transitLeg = new TransitPathLeg<>(
                    from.stop(),
                    to.departureTime(),
                    to.stop(),
                    to.arrivalTime(),
                    to.trip(),
                    lastLeg
            );

            if (from.arrivedByTransfer()) {
                to = from;
                from = from.previous();

                lastLeg = new TransferPathLeg<>(
                        from.stop(),
                        to.departureTime(),
                        to.stop(),
                        to.arrivalTime(),
                        transitLeg
                );
            } else {
                lastLeg = transitLeg;
            }
        }
        while (from.arrivedByTransit());

        AccessPathLeg<T> accessLeg = createAccessPathLeg(from, transitLeg);

        return new Path<>(
                accessLeg,
                destinationArrival.arrivalTime(),
                numberOfTransits - 1,
                CostCalculator.toOtpDomainCost(destinationArrival.cost())
        );
    }

    private AccessPathLeg<T> createAccessPathLeg(ArrivalView<T> from, TransitPathLeg<T> transitLeg) {
        int boardTimeTransit = transitLeg.fromTime();
        int accessDurationInSeconds = from.arrivalTime() - from.departureTime();
        int originDepartureTime = calculator.originDepartureTime(boardTimeTransit, accessDurationInSeconds);
        int accessArrivalTime = originDepartureTime + accessDurationInSeconds;

        return new AccessPathLeg<>(originDepartureTime, from.stop(), accessArrivalTime, transitLeg);
    }
}

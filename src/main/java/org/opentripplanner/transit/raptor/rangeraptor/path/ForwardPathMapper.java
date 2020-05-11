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
 */
public final class ForwardPathMapper<T extends RaptorTripSchedule> implements PathMapper<T> {
    private int iterationDepartureTime = -1;

    /**
     * Note! This is not the Raptor internal SlackProvider, but the slack-provider
     * submitted by the transit layer.
     */
    private final RaptorSlackProvider transitLayerSlackProvider;


    public ForwardPathMapper(RaptorSlackProvider slackProvider, WorkerLifeCycle lifeCycle) {
        this.transitLayerSlackProvider = slackProvider;
        lifeCycle.onSetupIteration(this::setRangeRaptorIterationDepartureTime);
    }

    private void setRangeRaptorIterationDepartureTime(int iterationDepartureTime) {
        this.iterationDepartureTime = iterationDepartureTime;
    }

    @Override
    public Path<T> mapToPath(final DestinationArrival<T> destinationArrival) {
        ArrivalView<T> from;
        ArrivalView<T> to;
        PathLeg<T> lastLeg;
        TransitPathLeg<T> transitLeg;

        from = destinationArrival.previous();
        lastLeg = new EgressPathLeg<>(
                destinationArrival.accessEgress(),
                from.stop(),
                destinationArrival.departureTime(),
                destinationArrival.arrivalTime()
        );

        do {
            to = from;
            from = from.previous();

            TripTimesSearch.Result r = TripTimesSearch.searchAfterEDT(
                    to.trip(),
                    from.stop(),
                    to.stop(),
                    from.arrivalTime()
            );

            transitLeg = new TransitPathLeg<>(
                    from.stop(),
                    r.boardTime,
                    to.stop(),
                    r.alightTime,
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

        return new Path<>(iterationDepartureTime, accessLeg, RaptorCostConverter.toOtpDomainCost(destinationArrival.cost()));
    }

    private AccessPathLeg<T> createAccessPathLeg(ArrivalView<T> from, TransitPathLeg<T> nextLeg) {
        int boardSlack = transitLayerSlackProvider.boardSlack(nextLeg.trip().pattern());
        int arrivalTime = nextLeg.fromTime() - boardSlack;
        int timeShift = arrivalTime - from.arrivalTime();
        int departureTime = from.departureTime() + timeShift;

        return new AccessPathLeg<>(from.accessEgress(), from.stop(), departureTime, arrivalTime, nextLeg);
    }
}

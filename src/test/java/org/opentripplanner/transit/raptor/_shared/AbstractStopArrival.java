package org.opentripplanner.transit.raptor._shared;



import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;

abstract class AbstractStopArrival implements ArrivalView<TestRaptorTripSchedule> {
    private final int round;
    private final int stop;
    private final int departureTime;
    private final int arrivalTime;
    private final int cost;
    private final ArrivalView<TestRaptorTripSchedule> previous;

    AbstractStopArrival(
            int round,
            int stop,
            int departureTime,
            int arrivalTime,
            int extraCost,
            ArrivalView<TestRaptorTripSchedule> previous
    ) {
        this.round = round;
        this.stop = stop;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.cost = (previous==null ? 0 : previous.cost()) + extraCost;
        this.previous = previous;
    }

    @Override
    public RaptorTransfer accessEgress() {
        return null;
    }

    @Override public int stop() { return stop; }
    @Override public int round() { return round; }
    @Override public int departureTime() { return departureTime; }
    @Override public int arrivalTime() { return arrivalTime; }
    @Override public int cost() { return cost; }
    @Override public ArrivalView<TestRaptorTripSchedule> previous() { return previous; }
    @Override public String toString() { return asString(); }
}

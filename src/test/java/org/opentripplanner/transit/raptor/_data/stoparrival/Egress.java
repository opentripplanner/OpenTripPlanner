package org.opentripplanner.transit.raptor._data.stoparrival;

import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.util.TimeUtils;

public class Egress {
    private final int arrivalTime;
    private final int durationInSeconds;
    private final ArrivalView<TestTripSchedule> previous;

    Egress(int departureTime, int arrivalTime, ArrivalView<TestTripSchedule> previous) {
        this.arrivalTime = arrivalTime;
        this.durationInSeconds = Math.abs(arrivalTime - departureTime);
        this.previous = previous;
    }

    public int additionalCost(){ return 1000; }
    public int durationInSeconds() { return durationInSeconds; }
    public int arrivalTime() { return arrivalTime; }
    public ArrivalView<TestTripSchedule> previous() { return previous; }

    @Override
    public String toString() {
            return String.format(
                    "Egress { round: %d, stop: %d, arrival-time: %s, cost: %d }",
                    previous.round(),
                    previous.stop(),
                    TimeUtils.timeToStrCompact(arrivalTime),
                    previous.cost() + additionalCost()
            );
    }
}

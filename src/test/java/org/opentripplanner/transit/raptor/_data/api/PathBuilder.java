package org.opentripplanner.transit.raptor._data.api;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.util.time.TimeUtils;


/**
 * Utility to help build paths for testing. The path builder is "reusable",
 * every time the {@code access(...)} methods are called the builder reset it self.
 */
public class PathBuilder {
  private static final int NOT_SET = -1;
  private static final int BOARD_ALIGHT_OFFSET = 30;

  private final int alightSlack;
  private final CostCalculator<TestTripSchedule> costCalculator;

  private final List<Leg> legs = new ArrayList<>();
  private int startTime;

  public PathBuilder(int alightSlack, CostCalculator<TestTripSchedule> costCalculator) {
    this.alightSlack = alightSlack;
    this.costCalculator = costCalculator;
  }

  public PathBuilder access(int startTime, int duration, int toStop) {
    return access(startTime, TestTransfer.walk(toStop, duration));
  }

  public PathBuilder access(int startTime, TestTransfer transfer) {
    reset(startTime);
    legs.add(new Leg(startTime, NOT_SET, transfer));
    return this;
  }

  public PathBuilder walk(int duration, int toStop) {
    return transfer(TestTransfer.walk(toStop, duration));
  }

  public PathBuilder walk(int duration, int toStop, int cost) {
    return transfer(TestTransfer.walk(toStop, duration, cost));
  }

  public PathBuilder bus(TestTripSchedule trip, int toStop) {
    int fromStop = prev().toStop;
    int fromTime = trip.departure(trip.pattern().findStopPositionAfter(0, fromStop));
    int toTime = trip.arrival(trip.pattern().findStopPositionAfter(0, toStop));

    return transit(fromStop, fromTime, toStop, toTime, trip);
  }

  public PathBuilder bus(String patternName, int fromTime, int duration, int toStop) {
    int fromStop = prev().toStop;
    int toTime = fromTime + duration;

    TestTripSchedule trip = TestTripSchedule
        .schedule(TestTripPattern.pattern(patternName, fromStop, toStop))
        .arrDepOffset(BOARD_ALIGHT_OFFSET)
        .departures(fromTime, toTime + BOARD_ALIGHT_OFFSET)
        .build();

    return transit(fromStop, fromTime, toStop, toTime, trip);
  }

  public Path<TestTripSchedule> egress(int duration) {
    return egress(TestTransfer.walk(prev().toStop, duration));
  }

  public Path<TestTripSchedule> egress(TestTransfer transfer) {
    return transfer(transfer).build();
  }

  /* private methods */

  private void reset(int startTime) {
    this.startTime = startTime;
    this.legs.clear();
  }

  private Path<TestTripSchedule> build() {
    return new Path<>(
        startTime,
        legs.get(0).accessLeg(leg(1))
    );
  }

  private Leg prev() {
    return legs.get(legs.size()-1);
  }

  private PathLeg<TestTripSchedule> leg(int index) {
    Leg leg = legs.get(index);

    if(index == legs.size()-1) {
      return leg.egressLeg();
    }
    else if(leg.isTransit()) {
      int waitTime = transitWaitTime(index);
      boolean firstTransit = index == 1 && !legs.get(0).transfer.hasRides();
      @SuppressWarnings("ConstantConditions")
      int cost  = costCalculator.transitArrivalCost(
              firstTransit, leg.fromStop, waitTime, leg.duration(),
              leg.trip.transitReluctanceFactorIndex(),
              leg.toStop
      );
      return leg.transitLeg(leg(index+1), cost);
    }
    else {
      return leg.transferLeg(leg(index+1));
    }
  }

  private PathBuilder transfer(TestTransfer transfer) {
    int fromStop = prev().toStop;
    int fromTime = prev().toTime + alightSlack;
    legs.add(new Leg(fromTime, fromStop, transfer));
    return this;
  }

  private PathBuilder transit(
          int fromStop, int fromTime, int toStop, int toTime, @NotNull TestTripSchedule trip
  ) {
    legs.add(new Leg(fromTime, fromStop, toTime, toStop, trip));
    return this;
  }

  private int transitWaitTime(int index) {
    Leg curr = legs.get(index);
    Leg prev = legs.get(index-1);

    if(prev.isTransit()) {
      // We can ignore alight-slack here, because the it should be added to the
      // previous toTime to find stop-arrival-time, and then the stop-arrival-time is
      // subtracted from the current fromTime plus alight-slack.
      return curr.fromTime - prev.toTime;
    }
    return (curr.fromTime - prev.toTime) + alightSlack;
  }

  private static class Leg {
    final int fromTime;
    final int fromStop;
    final int toTime;
    final int toStop;
    final TestTripSchedule trip;
    final TestTransfer transfer;

    Leg(
        int fromTime,
        int fromStop,
        TestTransfer transfer
    ) {
      this.fromTime = fromTime;
      this.fromStop = fromStop;
      this.toTime = fromTime + transfer.durationInSeconds();
      this.toStop = transfer.stop();
      this.transfer = transfer;
      this.trip = null;
    }

    Leg(
        int fromTime, int fromStop,
        int toTime, int toStop,
        TestTripSchedule trip
    ) {
      this.fromTime = fromTime;
      this.fromStop = fromStop;
      this.toTime = toTime;
      this.toStop = toStop;
      this.transfer = null;
      this.trip = trip;
    }

    AccessPathLeg<TestTripSchedule> accessLeg(PathLeg<TestTripSchedule> next) {
      var durationInSeconds = toTime - fromTime;
      return new AccessPathLeg<>(
          TestTransfer.walk(toStop, durationInSeconds),
          fromTime, toTime, next
      );
    }

    TransitPathLeg<TestTripSchedule> transitLeg(PathLeg<TestTripSchedule> next, int cost) {
      return new TransitPathLeg<>(
          fromStop, fromTime, toStop, toTime, cost, trip, next
      );
    }

    TransferPathLeg<TestTripSchedule> transferLeg(PathLeg<TestTripSchedule> next) {
      var durationInSeconds = toTime - fromTime;
      return new TransferPathLeg<>(
          fromStop, fromTime, toTime,
          TestTransfer.walk(toStop, durationInSeconds),
          next
      );
    }

    EgressPathLeg<TestTripSchedule> egressLeg() {
      var durationInSeconds = toTime - fromTime;
      return new EgressPathLeg<>(
          TestTransfer.walk(toStop, durationInSeconds),
          fromTime, toTime
      );
    }

    int duration() { return toTime - fromTime; }

    boolean isTransit() {
      return trip != null;
    }

    @Override
    public String toString() {
      ToStringBuilder text = ToStringBuilder
          .of(Leg.class)
          .addObj("from", fromStop + "  " + TimeUtils.timeToStrLong(fromTime))
          .addObj("to", toStop + " ~ " + TimeUtils.timeToStrLong(toTime));
      if(trip != null) {
        text.addObj("trip", trip.pattern().debugInfo());
      }
      if(transfer != null) {
        text.addObj("transfer", transfer);
      }
      return text.toString();
    }
  }
}

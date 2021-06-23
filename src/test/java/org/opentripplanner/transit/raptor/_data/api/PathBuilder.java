package org.opentripplanner.transit.raptor._data.api;

import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.walkCost;

import java.util.ArrayList;
import java.util.List;
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
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.util.time.TimeUtils;


/**
 * Utility to help build paths for testing
 */
public class PathBuilder {
  private static final int NOT_SET = -1;
  private static final int BOARD_ALIGHT_OFFSET = 30;

  private final int alightSlack;
  private final CostCalculator<TestTripSchedule> costCalculator;

  private final List<Leg> legs = new ArrayList<>();
  private int startTime;

  /**
   * This is true until the first transit leg is added, also remember to set this to false
   * if a flex access leg is added.
   */
  private boolean firstTransit = true;

  public PathBuilder(int alightSlack, CostCalculator<TestTripSchedule> costCalculator) {
    this.alightSlack = alightSlack;
    this.costCalculator = costCalculator;
  }

  public PathBuilder access(int startTime, int duration, int toStop) {
    return access(startTime, duration, toStop, walkCost(duration));
  }

  public PathBuilder access(int startTime, int duration, int toStop, int cost) {
    start(startTime);
    int toTime = startTime + duration;
    return leg(NOT_SET, startTime, toStop, toTime, cost, null);
  }

  public PathBuilder walk(int duration, int toStop) {
    return walk(duration, toStop, walkCost(duration));
  }

  public PathBuilder walk(int duration, int toStop, int cost) {
    int fromStop = prev().toStop;
    int fromTime = prev().toTime + alightSlack;
    int toTime = fromTime + duration;

    return leg(fromStop, fromTime, toStop, toTime, cost, null);
  }

  public PathBuilder bus(TestTripSchedule trip, int toStop) {
    int fromStop = prev().toStop;
    int fromTime = trip.departure(trip.pattern().findStopPositionAfter(0, fromStop));
    int toTime = trip.arrival(trip.pattern().findStopPositionAfter(0, toStop));

    int waitTime = currentTransitWaitTime(fromTime);
    int transitTime = toTime - fromTime;

    int cost  = costCalculator.transitArrivalCost(
        firstTransit(), fromStop, waitTime, transitTime, trip.transitReluctanceFactorIndex(), toStop
    );

    return leg(fromStop, fromTime, toStop, toTime, cost, trip);
  }

  public PathBuilder bus(String patternName, int fromTime, int duration, int toStop) {
    int fromStop = prev().toStop;
    int toTime = fromTime + duration;
    int waitTime = currentTransitWaitTime(fromTime);

    TestTripSchedule trip = TestTripSchedule
        .schedule(TestTripPattern.pattern(patternName, fromStop, toStop))
        .arrDepOffset(BOARD_ALIGHT_OFFSET)
        .departures(fromTime, toTime + BOARD_ALIGHT_OFFSET)
        .build();

    int cost  = costCalculator.transitArrivalCost(
        firstTransit(), fromStop, waitTime, duration, trip.transitReluctanceFactorIndex(), toStop
    );

    return leg(fromStop, fromTime, toStop, toTime, cost, trip);
  }

  public Path<TestTripSchedule> egress(int duration) {
    return egress(duration, walkCost(duration));
  }

  public Path<TestTripSchedule> egress(int duration, int cost) {
    return walk(duration, NOT_SET, cost).build();
  }


  /* private methods */

  private boolean firstTransit() {
    boolean temp = firstTransit;
    firstTransit = false;
    return temp;
  }

  private void start(int startTime) {
    this.startTime = startTime;
    this.legs.clear();
    this.firstTransit = true;
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

  int currentTransitWaitTime(int fromTime) {
    if(prev().isTransit()) {
      // We can ignore alight-slack here, because the it should be added to the
      // previous toTime to find stop-arrival-time, and then the stop-arrival-time is
      // subtracted from the current fromTime plus alight-slack.
      return fromTime - prev().toTime;
    }
    return (fromTime - prev().toTime) + alightSlack;
  }

  private PathLeg<TestTripSchedule> leg(int index) {
    Leg leg = legs.get(index);

    if(index == legs.size()-1) {
      return leg.egressLeg();
    }
    else if(leg.trip != null) {
      return leg.transitLeg(leg(index+1));
    }
    else {
      return leg.transferLeg(leg(index+1));
    }
  }

  private PathBuilder leg(
      int fromStop, int fromTime, int toStop, int toTime, int cost, TestTripSchedule trip
  ) {
    legs.add(new Leg(fromTime, fromStop, toTime, toStop, cost, trip));
    return this;
  }

  private static class Leg {
    final int fromTime;
    final int fromStop;
    final int toTime;
    final int toStop;
    final int raptorCost;
    final TestTripSchedule trip;

    Leg(
        int fromTime, int fromStop, int toTime, int toStop, int raptorCost, TestTripSchedule trip
    ) {
      this.fromTime = fromTime;
      this.fromStop = fromStop;
      this.toTime = toTime;
      this.toStop = toStop;
      this.raptorCost = raptorCost;
      this.trip = trip;
    }

    AccessPathLeg<TestTripSchedule> accessLeg(PathLeg<TestTripSchedule> next) {
      var durationInSeconds = toTime - fromTime;
      return new AccessPathLeg<>(
          TestTransfer.walk(toStop, durationInSeconds, walkCost(durationInSeconds)),
          toStop, fromTime, toTime, raptorCost, next
      );
    }

    TransitPathLeg<TestTripSchedule> transitLeg(PathLeg<TestTripSchedule> next) {
      return new TransitPathLeg<>(
          fromStop, fromTime, toStop, toTime, raptorCost, trip, next
      );
    }

    TransferPathLeg<TestTripSchedule> transferLeg(PathLeg<TestTripSchedule> next) {
      var durationInSeconds = toTime - fromTime;
      return new TransferPathLeg<>(
          fromStop, fromTime, toStop, toTime, raptorCost,
          TestTransfer.walk(toStop, durationInSeconds, walkCost(durationInSeconds)),
          next
      );
    }

    EgressPathLeg<TestTripSchedule> egressLeg() {
      var durationInSeconds = toTime - fromTime;
      return new EgressPathLeg<>(
          TestTransfer.walk(toStop, durationInSeconds, walkCost(durationInSeconds)),
          fromStop, fromTime, toTime, raptorCost
      );
    }

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
      return text.addNum("cost", raptorCost, 0).toString();
    }

    private int domainCost() {
      return RaptorCostConverter.toOtpDomainCost(raptorCost);
    }
  }
}

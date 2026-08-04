package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder.createGraphPath;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;

import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;

class CarpoolAccessEgressTest {

  private static final int STOP = 0;
  private static final Duration STOP_DURATION = Duration.ofMinutes(2);
  private static final int DWELL_SECONDS = (int) STOP_DURATION.getSeconds();
  private static final int PICKUP_POSITION = 1;
  private static final int DROPOFF_POSITION = 2;
  private static final Duration PICKUP_SEGMENT_DURATION = Duration.ofMinutes(1);

  /**
   * Walk time must be billed at the walk path's own A* weight (which already encodes walk
   * reluctance, safety, slope, ...) and the carpool ride at {@code carpoolReluctance}. The ride
   * includes the boarding dwell at the pickup stop.
   */
  @Test
  void c1AddsWalkPathWeightsAndChargesRideAtCarpoolReluctance() {
    var walkToPickup = createGraphPath(Duration.ofSeconds(80));
    var walkFromDropoff = createGraphPath(Duration.ofSeconds(40));
    double carpoolReluctance = 1.0;

    var accessEgress = newAccessEgress(
      1_000,
      walkToPickup,
      Duration.ofSeconds(60),
      walkFromDropoff,
      carpoolReluctance
    );

    int rideSeconds = 60 + DWELL_SECONDS;
    double expectedWeight =
      walkToPickup.getWeight() + walkFromDropoff.getWeight() + rideSeconds * carpoolReluctance;
    assertEquals(RaptorCostConverter.toRaptorCost(expectedWeight), accessEgress.c1());
  }

  /** With no walk, the cost reduces to {@code (sharedSegmentSeconds + dwell) * carpoolReluctance}. */
  @Test
  void c1WithoutWalkUsesOnlyCarpoolReluctance() {
    var accessEgress = newAccessEgress(0, null, Duration.ofSeconds(300), null, 1.5);

    int rideSeconds = 300 + DWELL_SECONDS;
    assertEquals(RaptorCostConverter.toRaptorCost(rideSeconds * 1.5), accessEgress.c1());
  }

  @Test
  void durationInSecondsCoversWalksAndRide() {
    var accessEgress = newAccessEgress(
      1_000,
      createGraphPath(Duration.ofSeconds(80)),
      Duration.ofSeconds(60),
      createGraphPath(Duration.ofSeconds(40)),
      1.0
    );

    int expectedDuration = 80 + 60 + DWELL_SECONDS + 40;
    assertEquals(expectedDuration, accessEgress.durationInSeconds());
    assertEquals(1_000, accessEgress.getPassengerDepartureTime());
    assertEquals(1_000 + expectedDuration, accessEgress.getPassengerArrivalTime());
  }

  /**
   * {@code withPenalty} installs the new penalty and preserves the leg's stop and time anchors.
   * Mirroring {@code DefaultAccessEgress}, the penalty's cost is folded into {@link
   * CarpoolAccessEgress#c1()} (Raptor itself does not propagate the time-penalty into c1) and the
   * penalty's time is exposed via {@link CarpoolAccessEgress#timePenalty()}. The wall-clock
   * {@code durationInSeconds} is unchanged because the time-penalty is virtual time inside Raptor,
   * not part of the leg's actual duration.
   */
  @Test
  void withPenaltyFoldsCostIntoC1AndExposesTimePenalty() {
    var original = newAccessEgress(
      1_000,
      createGraphPath(Duration.ofSeconds(80)),
      Duration.ofSeconds(60),
      createGraphPath(Duration.ofSeconds(40)),
      1.0
    );
    var newPenalty = new TimeAndCost(Duration.ofSeconds(30), Cost.costOfSeconds(45));

    var withPenalty = (CarpoolAccessEgress) original.withPenalty(newPenalty);

    assertEquals(newPenalty, withPenalty.penalty());
    assertEquals(original.stop(), withPenalty.stop());
    assertEquals(original.c1() + newPenalty.cost().toCentiSeconds(), withPenalty.c1());
    assertEquals((int) newPenalty.time().toSeconds(), withPenalty.timePenalty());
    assertEquals(original.durationInSeconds(), withPenalty.durationInSeconds());
    assertEquals(original.getPassengerDepartureTime(), withPenalty.getPassengerDepartureTime());
    assertEquals(original.getPassengerArrivalTime(), withPenalty.getPassengerArrivalTime());
  }

  /** Without a penalty, {@code timePenalty()} returns the Raptor sentinel {@code TIME_NOT_SET}. */
  @Test
  void timePenaltyDefaultsToTimeNotSet() {
    var subject = newAccessEgress(0, null, Duration.ofSeconds(300), null, 1.0);

    assertEquals(RaptorConstants.TIME_NOT_SET, subject.timePenalty());
  }

  /**
   * {@code withPenalty} is a one-shot decoration. Calling it on a leg that already has a non-zero
   * penalty would otherwise re-fold a cost into {@code c1} and silently discard the previous
   * penalty, so the second application throws — matching {@code DefaultAccessEgress}.
   */
  @Test
  void canNotAddPenaltyTwice() {
    var subject = newAccessEgress(
      1_000,
      createGraphPath(Duration.ofSeconds(80)),
      Duration.ofSeconds(60),
      createGraphPath(Duration.ofSeconds(40)),
      1.0
    );
    var penalty = new TimeAndCost(Duration.ofSeconds(30), Cost.costOfSeconds(45));
    var withPenalty = subject.withPenalty(penalty);

    assertThrows(IllegalStateException.class, () -> withPenalty.withPenalty(penalty));
  }

  /**
   * For an access the transit stop sits at the chain end, so the final state is the last state of
   * the trailing walk from the dropoff to the stop.
   */
  @Test
  void getFinalStateForAccessReturnsStateAtTransitStopChainEnd() {
    var walkToPickup = createGraphPath(Duration.ofSeconds(80));
    var walkFromDropoff = createGraphPath(Duration.ofSeconds(40));
    var stop = TransitRepositoryForTest.of().stop("Central Station", 59.91, 10.74).build();

    var access = newAccessEgress(
      1_000,
      walkToPickup,
      Duration.ofSeconds(60),
      walkFromDropoff,
      1.0,
      EndpointLabel.forLocation(GenericLocation.fromCoordinate(59.92, 10.75, "Home")),
      EndpointLabel.forStop(stop)
    );

    assertEquals(walkFromDropoff.states.getLast(), access.getFinalState());
  }

  /**
   * For an egress the transit stop sits at the chain start, so the final state is the first state
   * of the leading walk from the stop to the pickup.
   */
  @Test
  void getFinalStateForEgressReturnsStateAtTransitStopChainStart() {
    var walkToPickup = createGraphPath(Duration.ofSeconds(80));
    var walkFromDropoff = createGraphPath(Duration.ofSeconds(40));
    var stop = TransitRepositoryForTest.of().stop("Central Station", 59.91, 10.74).build();

    var egress = newAccessEgress(
      1_000,
      walkToPickup,
      Duration.ofSeconds(60),
      walkFromDropoff,
      1.0,
      EndpointLabel.forStop(stop),
      EndpointLabel.forLocation(GenericLocation.fromCoordinate(59.92, 10.75, "Office"))
    );

    assertEquals(walkToPickup.states.getFirst(), egress.getFinalState());
  }

  /**
   * With no walks bracketing the ride, the final state falls back to the shared ride segment
   * endpoint at the transit stop — the last shared segment's last state for an access.
   */
  @Test
  void getFinalStateWithoutWalksFallsBackToSharedSegment() {
    var access = newAccessEgress(
      0,
      null,
      Duration.ofSeconds(300),
      null,
      1.0,
      EndpointLabel.EMPTY,
      EndpointLabel.forStop(TransitRepositoryForTest.of().stop("Stop", 59.91, 10.74).build())
    );

    assertEquals(access.sharedSegments().getLast().states.getLast(), access.getFinalState());
  }

  /** A carpool passenger rides in the driver's car, never a station-rented vehicle. */
  @Test
  void getFinalStateIsNotRentingVehicleFromStation() {
    var access = newAccessEgress(
      1_000,
      createGraphPath(Duration.ofSeconds(80)),
      Duration.ofSeconds(60),
      createGraphPath(Duration.ofSeconds(40)),
      1.0
    );

    assertFalse(access.getFinalState().isRentingVehicleFromStation());
  }

  /**
   * Builds a CarpoolAccessEgress with the passenger picked up mid-trip (pickupPos = 1, dropoffPos
   * = 2). The route segments list is therefore [pickupSegment, sharedSegment]: the driver runs
   * the first to reach the passenger and the second is the passenger's shared ride. The
   * passenger's ride duration is {@code sharedSegmentDuration + STOP_DURATION} — the boarding
   * dwell at the pickup stop is part of the ride.
   */
  private static CarpoolAccessEgress newAccessEgress(
    int passengerDepartureTime,
    @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
    Duration sharedSegmentDuration,
    @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff,
    double carpoolReluctance
  ) {
    return newAccessEgress(
      passengerDepartureTime,
      walkToPickup,
      sharedSegmentDuration,
      walkFromDropoff,
      carpoolReluctance,
      EndpointLabel.EMPTY,
      EndpointLabel.EMPTY
    );
  }

  private static CarpoolAccessEgress newAccessEgress(
    int passengerDepartureTime,
    @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
    Duration sharedSegmentDuration,
    @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff,
    double carpoolReluctance,
    EndpointLabel startLabel,
    EndpointLabel endLabel
  ) {
    var candidate = new InsertionCandidate(
      createSimpleTrip(OSLO_CENTER, OSLO_NORTH),
      PICKUP_POSITION,
      DROPOFF_POSITION,
      List.of(createGraphPath(PICKUP_SEGMENT_DURATION), createGraphPath(sharedSegmentDuration)),
      STOP_DURATION,
      null,
      walkToPickup,
      walkFromDropoff
    );
    return new CarpoolAccessEgress(
      STOP,
      passengerDepartureTime,
      candidate,
      TimeAndCost.ZERO,
      carpoolReluctance,
      startLabel,
      endLabel
    );
  }
}

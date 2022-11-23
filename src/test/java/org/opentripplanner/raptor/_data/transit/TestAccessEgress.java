package org.opentripplanner.raptor._data.transit;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter.toRaptorCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;

/**
 * Simple implementation for {@link RaptorAccessEgress} for use in unit-tests.
 */
public class TestAccessEgress implements RaptorAccessEgress {

  public static final int SECONDS_IN_DAY = 24 * 3600;
  public static final int DEFAULT_NUMBER_OF_RIDES = 0;
  public static final boolean STOP_REACHED_ON_BOARD = true;
  public static final boolean STOP_REACHED_ON_FOOT = false;
  public static final double DEFAULT_WALK_RELUCTANCE = 2.0;

  private final int stop;
  private final int durationInSeconds;
  private final int cost;
  private final int numberOfRides;
  private final boolean stopReachedOnBoard;
  private final boolean isEmpty;
  private final Integer opening;
  private final Integer closing;

  private TestAccessEgress(Builder builder) {
    this.stop = builder.stop;
    this.durationInSeconds = builder.durationInSeconds;
    this.cost = builder.cost;
    this.numberOfRides = builder.numberOfRides;
    this.stopReachedOnBoard = builder.stopReachedOnBoard;
    this.isEmpty = builder.isEmpty;
    this.opening = builder.opening;
    this.closing = builder.closing;
  }

  public static TestAccessEgress walk(int stop, int durationInSeconds) {
    return new Builder(stop, durationInSeconds).build();
  }

  public static TestAccessEgress walk(int stop, int durationInSeconds, double walkReluctance) {
    return walk(stop, durationInSeconds, walkCost(durationInSeconds, walkReluctance));
  }

  public static TestAccessEgress walk(int stop, int durationInSeconds, int cost) {
    return new Builder(stop, durationInSeconds).withCost(cost).build();
  }

  public static TestAccessEgress flexWithOnBoard(int stop, int durationInSeconds, int cost) {
    return new Builder(stop, durationInSeconds)
      .withCost(cost)
      .withNRides(1)
      .stopReachedOnBoard()
      .build();
  }

  public static TestAccessEgress zeroDurationAccess(int stop, int cost) {
    return new Builder(stop, 0).withIsEmpty(true).withCost(cost).build();
  }

  /**
   * Creates a walk transfer with time restrictions. opening and closing may be specified as seconds
   * since the start of "RAPTOR time" to limit the time periods that the access is traversable,
   * which is repeatead every 24 hours. This allows the access to only be traversable between for
   * example 08:00 and 16:00 every day.
   */
  public static TestAccessEgress walk(int stop, int durationInSeconds, int opening, int closing) {
    return new Builder(stop, durationInSeconds).withOpeningHours(opening, closing).build();
  }

  public static TestAccessEgress walk(
    int stop,
    int durationInSeconds,
    int cost,
    int opening,
    int closing
  ) {
    return new Builder(stop, durationInSeconds)
      .withCost(cost)
      .withOpeningHours(opening, closing)
      .build();
  }

  /** Create a new flex access and arrive stop onBoard with 1 ride/extra transfer. */
  public static TestAccessEgress flex(int stop, int durationInSeconds) {
    return flex(stop, durationInSeconds, 1, walkCost(durationInSeconds));
  }

  /** Create a new flex access and arrive stop onBoard with 1 ride/extra transfer. */
  public static TestAccessEgress flex(int stop, int durationInSeconds, int nRides) {
    return flex(stop, durationInSeconds, nRides, walkCost(durationInSeconds));
  }

  /** Create a new flex access and arrive stop onBoard. */
  public static TestAccessEgress flex(int stop, int durationInSeconds, int nRides, int cost) {
    assert nRides > DEFAULT_NUMBER_OF_RIDES;
    return new Builder(stop, durationInSeconds)
      .stopReachedOnBoard()
      .withNRides(nRides)
      .withCost(cost)
      .build();
  }

  /** Create a flex access arriving at given stop by walking with 1 ride/extra transfer. */
  public static TestAccessEgress flexAndWalk(int stop, int durationInSeconds) {
    return flexAndWalk(stop, durationInSeconds, 1, walkCost(durationInSeconds));
  }

  /** Create a flex access arriving at given stop by walking with 1 ride/extra transfer. */
  public static TestAccessEgress flexAndWalk(int stop, int durationInSeconds, int nRides) {
    return flexAndWalk(stop, durationInSeconds, nRides, walkCost(durationInSeconds));
  }

  /** Create a flex access arriving at given stop by walking. */
  public static TestAccessEgress flexAndWalk(
    int stop,
    int durationInSeconds,
    int nRides,
    int cost
  ) {
    assert nRides > DEFAULT_NUMBER_OF_RIDES;
    return new Builder(stop, durationInSeconds).withNRides(nRides).withCost(cost).build();
  }

  public static Collection<RaptorAccessEgress> transfers(int... stopTimes) {
    List<RaptorAccessEgress> legs = new ArrayList<>();
    for (int i = 0; i < stopTimes.length; i += 2) {
      legs.add(walk(stopTimes[i], stopTimes[i + 1]));
    }
    return legs;
  }

  public static int walkCost(int durationInSeconds) {
    return walkCost(durationInSeconds, DEFAULT_WALK_RELUCTANCE);
  }

  public static int walkCost(int durationInSeconds, double reluctance) {
    return toRaptorCost(durationInSeconds * reluctance);
  }

  /** Set opening and closing hours and return a new object. */
  public TestAccessEgress openingHours(int opening, int closing) {
    return new Builder(this).withOpeningHours(opening, closing).build();
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int generalizedCost() {
    return cost;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (!hasOpeningHours()) {
      return requestedDepartureTime;
    }
    if (isClosed()) {
      return -1;
    }

    int days = Math.floorDiv(requestedDepartureTime, SECONDS_IN_DAY);
    int specificOpening = days * SECONDS_IN_DAY + opening;
    int specificClosing = days * SECONDS_IN_DAY + closing;

    if (requestedDepartureTime < specificOpening) {
      return specificOpening;
    } else if (requestedDepartureTime > specificClosing) {
      // return the opening time for the next day
      return specificOpening + SECONDS_IN_DAY;
    }
    return requestedDepartureTime;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    if (!hasOpeningHours()) {
      return requestedArrivalTime;
    }
    if (isClosed()) {
      return -1;
    }

    // opening & closing is relative to the departure
    int requestedDepartureTime = requestedArrivalTime - durationInSeconds();
    int days = Math.floorDiv(requestedDepartureTime, SECONDS_IN_DAY);
    int specificOpening = days * SECONDS_IN_DAY + opening;
    int specificClosing = days * SECONDS_IN_DAY + closing;
    int closeAtArrival = specificClosing + durationInSeconds();

    if (requestedDepartureTime < specificOpening) {
      // return the closing for the previous day, offset with durationInSeconds()
      return closeAtArrival - SECONDS_IN_DAY;
    } else if (requestedArrivalTime > closeAtArrival) {
      return closeAtArrival;
    }
    return requestedArrivalTime;
  }

  @Override
  public boolean hasOpeningHours() {
    return opening != null || closing != null;
  }

  @Override
  public int numberOfRides() {
    return numberOfRides;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return stopReachedOnBoard;
  }

  @Override
  public boolean isEmpty() {
    return this.isEmpty;
  }

  @Override
  public String toString() {
    return asString();
  }

  /** To specify that the transit is closed use an opening AFTER closing */
  public boolean isClosed() {
    return opening > closing;
  }

  /**
   * Do not use the builder, use the static factory methods. Only use the builder if you need to
   * override the {@link TestAccessEgress class}.
   */
  protected static class Builder {

    int stop;
    int durationInSeconds;
    int cost;
    int numberOfRides = DEFAULT_NUMBER_OF_RIDES;
    boolean stopReachedOnBoard = STOP_REACHED_ON_FOOT;
    Integer opening = null;
    Integer closing = null;
    private boolean isEmpty;

    Builder(int stop, int durationInSeconds) {
      this.stop = stop;
      this.durationInSeconds = durationInSeconds;
      this.cost = walkCost(durationInSeconds);
    }

    Builder(TestAccessEgress transfer) {
      this.stop = transfer.stop;
      this.durationInSeconds = transfer.durationInSeconds;
      this.stopReachedOnBoard = transfer.stopReachedOnBoard;
      this.cost = transfer.cost;
      this.numberOfRides = transfer.numberOfRides;
      this.opening = transfer.opening;
      this.closing = transfer.closing;
    }

    Builder withIsEmpty(boolean isEmpty) {
      this.isEmpty = isEmpty;
      return this;
    }

    Builder withCost(int cost) {
      this.cost = cost;
      return this;
    }

    Builder withNRides(int numberOfRides) {
      this.numberOfRides = numberOfRides;
      return this;
    }

    Builder stopReachedOnBoard() {
      this.stopReachedOnBoard = STOP_REACHED_ON_BOARD;
      return this;
    }

    Builder withOpeningHours(int opening, int closing) {
      this.opening = opening;
      this.closing = closing;
      return this;
    }

    TestAccessEgress build() {
      return new TestAccessEgress(this);
    }
  }
}

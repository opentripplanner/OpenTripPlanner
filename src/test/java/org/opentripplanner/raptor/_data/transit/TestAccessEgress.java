package org.opentripplanner.raptor._data.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.RaptorTestConstants.SECONDS_IN_A_DAY;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter.toRaptorCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;

/**
 * Simple implementation for {@link RaptorAccessEgress} for use in unit-tests.
 */
public class TestAccessEgress implements RaptorAccessEgress {

  public static final int DEFAULT_NUMBER_OF_RIDES = 0;
  public static final boolean STOP_REACHED_ON_BOARD = true;
  public static final boolean STOP_REACHED_ON_FOOT = false;
  public static final double DEFAULT_WALK_RELUCTANCE = 2.0;

  private final int stop;
  private final int durationInSeconds;
  private final int c1;
  private final int numberOfRides;
  private final boolean stopReachedOnBoard;
  private final boolean free;
  private final Integer opening;
  private final Integer closing;
  private final boolean closed;
  private final int timePenalty;

  private TestAccessEgress(Builder builder) {
    this.stop = builder.stop;
    this.durationInSeconds = builder.durationInSeconds;
    this.numberOfRides = builder.numberOfRides;
    this.stopReachedOnBoard = builder.stopReachedOnBoard;
    this.free = builder.free;
    this.opening = builder.opening;
    this.closing = builder.closing;
    this.closed = builder.closed;
    this.timePenalty = builder.timePenalty;
    this.c1 = builder.c1;

    if (free) {
      assertEquals(0, durationInSeconds);
    } else {
      assertTrue(durationInSeconds > 0);
    }
    if (closed) {
      assertNull(opening);
      assertNull(closing);
    }
    assertTrue(numberOfRides >= 0);
  }

  public static TestAccessEgress free(int stop) {
    return new Builder(stop, 0).withFree().build();
  }

  /**
   * @deprecated A stop cannot be both free and have a cost - This is not a valid
   *             access/egress.
   */
  @Deprecated
  public static TestAccessEgress free(int stop, int cost) {
    return new Builder(stop, 0).withFree().withCost(cost).build();
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

  /**
   * Add opening and closing hours and return a new object.
   * <p>
   * Opening and closing is specified as seconds since the start of "RAPTOR time" to limit the
   * time periods that the access is traversable, which is repeatead every 24 hours. This allows
   * access to only be traversable between given times like 08:00 and 16:00 every day.
   */
  public TestAccessEgress openingHours(int opening, int closing) {
    return copyOf().withOpeningHours(opening, closing).build();
  }

  /** Alias for {@code openingHours(TimeUtils.time(opening), TimeUtils.time(closing))} */
  public TestAccessEgress openingHours(String opening, String closing) {
    return openingHours(TimeUtils.time(opening), TimeUtils.time(closing));
  }

  public TestAccessEgress openingHoursClosed() {
    return copyOf().withClosed().build();
  }

  public TestAccessEgress withTimePenalty(int timePenalty) {
    return this.copyOf().withTimePenalty(timePenalty).build();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public int timePenalty() {
    return timePenalty;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (!hasOpeningHours()) {
      return requestedDepartureTime;
    }
    if (closed) {
      return RaptorConstants.TIME_NOT_SET;
    }

    int days = Math.floorDiv(requestedDepartureTime, SECONDS_IN_A_DAY);
    int specificOpening = days * SECONDS_IN_A_DAY + opening;
    int specificClosing = days * SECONDS_IN_A_DAY + closing;

    if (requestedDepartureTime < specificOpening) {
      return specificOpening;
    } else if (requestedDepartureTime > specificClosing) {
      // return the opening time for the next day
      return specificOpening + SECONDS_IN_A_DAY;
    }
    return requestedDepartureTime;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    if (!hasOpeningHours()) {
      return requestedArrivalTime;
    }
    if (closed) {
      return RaptorConstants.TIME_NOT_SET;
    }

    // opening & closing is relative to the departure
    int requestedDepartureTime = requestedArrivalTime - durationInSeconds();
    int days = Math.floorDiv(requestedDepartureTime, SECONDS_IN_A_DAY);
    int specificOpening = days * SECONDS_IN_A_DAY + opening;
    int specificClosing = days * SECONDS_IN_A_DAY + closing;
    int closeAtArrival = specificClosing + durationInSeconds();

    if (requestedDepartureTime < specificOpening) {
      // return the closing for the previous day, offset with durationInSeconds()
      return closeAtArrival - SECONDS_IN_A_DAY;
    } else if (requestedArrivalTime > closeAtArrival) {
      return closeAtArrival;
    }
    return requestedArrivalTime;
  }

  @Override
  public boolean hasOpeningHours() {
    return closed || opening != null || closing != null;
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
  public boolean isFree() {
    return this.free;
  }

  @Override
  public String toString() {
    return asString(true, true, null);
  }

  /**
   * Do not use the builder, use the static factory methods. Only use the builder if you need to
   * override the {@link TestAccessEgress class}.
   */
  protected static class Builder {

    int stop;
    int durationInSeconds;
    int c1;
    int numberOfRides = DEFAULT_NUMBER_OF_RIDES;
    boolean stopReachedOnBoard = STOP_REACHED_ON_FOOT;
    Integer opening = null;
    Integer closing = null;
    private boolean free = false;
    private boolean closed = false;
    private int timePenalty;

    Builder(int stop, int durationInSeconds) {
      this.stop = stop;
      this.durationInSeconds = durationInSeconds;
      this.c1 = walkCost(durationInSeconds);
      this.timePenalty = RaptorConstants.TIME_NOT_SET;
    }

    Builder(TestAccessEgress original) {
      this.free = original.free;
      this.stop = original.stop;
      this.durationInSeconds = original.durationInSeconds;
      this.stopReachedOnBoard = original.stopReachedOnBoard;
      this.c1 = original.c1;
      this.numberOfRides = original.numberOfRides;
      this.opening = original.opening;
      this.closing = original.closing;
      this.closed = original.closed;
      this.timePenalty = original.timePenalty;
    }

    Builder withFree() {
      this.free = true;
      this.durationInSeconds = 0;
      return this;
    }

    Builder withCost(int cost) {
      this.c1 = cost;
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

    Builder withTimePenalty(int timePenalty) {
      this.timePenalty = timePenalty;
      return this;
    }

    Builder withOpeningHours(int opening, int closing) {
      if (opening > closing) {
        throw new IllegalStateException(
          "Must open before is close. Opens at " +
          TimeUtils.timeToStrCompact(opening) +
          " and close at " +
          TimeUtils.timeToStrCompact(closing) +
          "."
        );
      }
      this.closed = false;
      this.opening = opening;
      this.closing = closing;
      return this;
    }

    Builder withClosed() {
      this.opening = null;
      this.closing = null;
      this.closed = true;
      return this;
    }

    TestAccessEgress build() {
      return new TestAccessEgress(this);
    }
  }
}

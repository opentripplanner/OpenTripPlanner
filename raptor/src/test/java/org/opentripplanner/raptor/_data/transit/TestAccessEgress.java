package org.opentripplanner.raptor._data.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.RaptorTestConstants.SECONDS_IN_A_DAY;
import static org.opentripplanner.raptor.api.model.RaptorConstants.isTimeSet;
import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorAccessEgressToStringParser;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorValue;
import org.opentripplanner.utils.time.TimeUtils;

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
  private final int openFrom;
  private final int openUntil;
  private final boolean closed;
  private final int timePenalty;
  private final int numberOfViaLocationsVisited;

  private TestAccessEgress(Builder builder) {
    this.stop = builder.stop;
    this.durationInSeconds = builder.durationInSeconds;
    this.numberOfRides = builder.numberOfRides;
    this.stopReachedOnBoard = builder.stopReachedOnBoard;
    this.free = builder.free;
    this.openFrom = builder.openFrom;
    this.openUntil = builder.openUntil;
    this.closed = builder.closed;
    this.timePenalty = builder.timePenalty;
    this.c1 = builder.c1;
    this.numberOfViaLocationsVisited = builder.numberOfViaLocationsVisited;

    if (free) {
      assertEquals(0, durationInSeconds);
    } else {
      assertTrue(durationInSeconds > 0);
    }
    if (closed) {
      assertTimeNotSet(openFrom, "'openFrom'");
      assertTimeNotSet(openUntil, "'openUntil'");
    }
    assertTrue(numberOfRides >= 0);
  }

  public static TestAccessEgress of(String text) {
    var data = RaptorAccessEgressToStringParser.parseAccessEgress(
      text,
      RaptorTestConstants::stopNameToIndex
    );
    return new Builder(data).build();
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

  public TestAccessEgress withCost(int c1) {
    return this.copyOf().withCost(c1).build();
  }

  public TestAccessEgress withTimePenalty(int timePenalty) {
    return this.copyOf().withTimePenalty(timePenalty).build();
  }

  public TestAccessEgress withViaLocationsVisited(int numberOfViaLocationsVisited) {
    return this.copyOf().withViaLocationsVisited(numberOfViaLocationsVisited).build();
  }

  @Override
  public int numberOfViaLocationsVisited() {
    return numberOfViaLocationsVisited;
  }

  protected Builder copyOf() {
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
    int specificOpenFrom = days * SECONDS_IN_A_DAY + openFrom;
    int specificOpenTo = days * SECONDS_IN_A_DAY + openUntil;

    if (requestedDepartureTime < specificOpenFrom) {
      return specificOpenFrom;
    } else if (requestedDepartureTime > specificOpenTo) {
      // return the opening time for the next day
      return specificOpenFrom + SECONDS_IN_A_DAY;
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
    int specificOpenFrom = days * SECONDS_IN_A_DAY + openFrom;
    int specificOpenTo = days * SECONDS_IN_A_DAY + openUntil;
    int closeAtArrival = specificOpenTo + durationInSeconds();

    if (requestedDepartureTime < specificOpenFrom) {
      // return the closing for the previous day, offset with durationInSeconds()
      return closeAtArrival - SECONDS_IN_A_DAY;
    } else if (requestedArrivalTime > closeAtArrival) {
      return closeAtArrival;
    }
    return requestedArrivalTime;
  }

  @Override
  public boolean hasOpeningHours() {
    return closed || (isTimeSet(openFrom) && isTimeSet(openUntil));
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
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestAccessEgress that = (TestAccessEgress) o;
    return (
      stop == that.stop &&
      durationInSeconds == that.durationInSeconds &&
      c1 == that.c1 &&
      numberOfRides == that.numberOfRides &&
      stopReachedOnBoard == that.stopReachedOnBoard &&
      timePenalty == that.timePenalty &&
      closed == that.closed &&
      openUntil == that.openUntil &&
      openFrom == that.openFrom
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      stop,
      durationInSeconds,
      c1,
      numberOfRides,
      stopReachedOnBoard,
      timePenalty,
      closed,
      openUntil,
      openFrom
    );
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
    int c1 = RaptorConstants.NOT_SET;
    int numberOfRides = DEFAULT_NUMBER_OF_RIDES;
    boolean stopReachedOnBoard = STOP_REACHED_ON_FOOT;
    int openFrom = RaptorConstants.TIME_NOT_SET;
    int openUntil = RaptorConstants.TIME_NOT_SET;
    private boolean free = false;
    private boolean closed = false;
    private int timePenalty = RaptorConstants.TIME_NOT_SET;
    private int numberOfViaLocationsVisited = RaptorConstants.ZERO;

    Builder(int stop, int durationInSeconds) {
      this.stop = stop;
      this.durationInSeconds = durationInSeconds;
      this.c1 = walkCost(durationInSeconds);
    }

    Builder(TestAccessEgress original) {
      this.free = original.free;
      this.stop = original.stop;
      this.durationInSeconds = original.durationInSeconds;
      this.stopReachedOnBoard = original.stopReachedOnBoard;
      this.c1 = original.c1;
      this.numberOfRides = original.numberOfRides;
      this.openFrom = original.openFrom;
      this.openUntil = original.openUntil;
      this.closed = original.closed;
      this.timePenalty = original.timePenalty;
      this.numberOfViaLocationsVisited = original.numberOfViaLocationsVisited;
    }

    public Builder(RaptorAccessEgressToStringParser data) {
      this.stop = data.stopIndex();
      this.durationInSeconds = data.duration();
      this.stopReachedOnBoard = data.isStopReachedOnBoard();
      this.free = data.isFree();
      this.closed = data.isClosed();
      this.openFrom = data.openFrom();
      this.openUntil = data.openTo();

      for (RaptorValue field : data.fields()) {
        switch (field.type()) {
          case C1 -> withCost(field.value());
          case RIDES -> withNRides(field.value());
          case TIME_PENALTY -> withTimePenalty(field.value());
          case VIAS -> withViaLocationsVisited(field.value());
          case C2,
            TRANSFERS,
            TRANSFER_PRIORITY,
            WAIT_TIME_COST -> throw new IllegalArgumentException(field.toString());
          default -> throw new IllegalArgumentException(field.type().toString());
        }
      }
      if (!RaptorConstants.isSet(c1)) {
        this.c1 = walkCost(durationInSeconds);
      }
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

    Builder withViaLocationsVisited(int numberOfViaLocationsVisited) {
      this.numberOfViaLocationsVisited = numberOfViaLocationsVisited;
      return this;
    }

    Builder withOpeningHours(int openFrom, int openUntil) {
      if (openFrom > openUntil) {
        throw new IllegalStateException(
          "Must open before is close. Opens at " +
          TimeUtils.timeToStrCompact(openFrom) +
          " and close at " +
          TimeUtils.timeToStrCompact(openUntil) +
          "."
        );
      }
      this.closed = false;
      this.openFrom = openFrom;
      this.openUntil = openUntil;
      return this;
    }

    Builder withClosed() {
      this.openFrom = RaptorConstants.TIME_NOT_SET;
      this.openUntil = RaptorConstants.TIME_NOT_SET;
      this.closed = true;
      return this;
    }

    TestAccessEgress build() {
      return new TestAccessEgress(this);
    }
  }

  public static void assertTimeNotSet(int time, String field) {
    if (isTimeSet(time)) {
      throw new IllegalArgumentException("Time '" + field + "' is set: " + time);
    }
  }
}

package org.opentripplanner.raptorlegacy._data.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.RaptorCostConverter.toRaptorCost;
import static org.opentripplanner.raptorlegacy._data.RaptorTestConstants.SECONDS_IN_A_DAY;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;


/**
 * Simple implementation for {@link RaptorAccessEgress} for use in unit-tests.
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
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


  /** Create a new flex access and arrive stop onBoard. */
  public static TestAccessEgress flex(int stop, int durationInSeconds, int nRides, int cost) {
    assert nRides > DEFAULT_NUMBER_OF_RIDES;
    return new Builder(stop, durationInSeconds)
      .stopReachedOnBoard()
      .withNRides(nRides)
      .withCost(cost)
      .build();
  }

  public static int walkCost(int durationInSeconds) {
    return walkCost(durationInSeconds, DEFAULT_WALK_RELUCTANCE);
  }

  public static int walkCost(int durationInSeconds, double reluctance) {
    return toRaptorCost(durationInSeconds * reluctance);
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

    TestAccessEgress build() {
      return new TestAccessEgress(this);
    }
  }
}

package org.opentripplanner.transit.raptor._data.transit;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter.toRaptorCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

/**
 * Simple implementation for {@link RaptorTransfer} for use in unit-tests.
 */
public class TestTransfer implements RaptorTransfer {

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
  private final Integer opening;
  private final Integer closing;

  private TestTransfer(Builder builder) {
    this.stop = builder.stop;
    this.durationInSeconds = builder.durationInSeconds;
    this.cost = builder.cost;
    this.numberOfRides = builder.numberOfRides;
    this.stopReachedOnBoard = builder.stopReachedOnBoard;
    this.opening = builder.opening;
    this.closing = builder.closing;
  }

  public static TestTransfer transfer(int stop, int durationInSeconds) {
    return new Builder(stop, durationInSeconds).build();
  }

  public static TestTransfer transfer(int stop, int durationInSeconds, double walkReluctance) {
    return transfer(stop, durationInSeconds, walkCost(durationInSeconds, walkReluctance));
  }

  public static TestTransfer transfer(int stop, int durationInSeconds, int cost) {
    return new Builder(stop, durationInSeconds).withCost(cost).build();
  }

  /**
   * Creates a walk transfer with time restrictions. opening and closing may be specified as seconds
   * since the start of "RAPTOR time" to limit the time periods that the access is traversable,
   * which is repeatead every 24 hours. This allows the access to only be traversable between for
   * example 08:00 and 16:00 every day.
   */
  public static TestTransfer transfer(int stop, int durationInSeconds, int opening, int closing) {
    return new Builder(stop, durationInSeconds).withOpeningHours(opening, closing).build();
  }

  public static TestTransfer transfer(
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

  public static Collection<TestTransfer> transfers(int... stopTimes) {
    List<TestTransfer> legs = new ArrayList<>();
    for (int i = 0; i < stopTimes.length; i += 2) {
      legs.add(transfer(stopTimes[i], stopTimes[i + 1]));
    }
    return legs;
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
  public int generalizedCost() {
    return cost;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
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
   * override the {@link TestTransfer class}.
   */
  protected static class Builder {

    int stop;
    int durationInSeconds;
    int cost;
    int numberOfRides = DEFAULT_NUMBER_OF_RIDES;
    boolean stopReachedOnBoard = STOP_REACHED_ON_FOOT;
    Integer opening = null;
    Integer closing = null;

    Builder(int stop, int durationInSeconds) {
      this.stop = stop;
      this.durationInSeconds = durationInSeconds;
      this.cost = walkCost(durationInSeconds);
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

    TestTransfer build() {
      return new TestTransfer(this);
    }
  }
}

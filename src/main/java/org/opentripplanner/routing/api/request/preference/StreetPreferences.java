package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.impl.DurationComparator;
import org.opentripplanner.routing.impl.PathComparator;
import org.opentripplanner.routing.spt.GraphPath;

// Direct street search
public class StreetPreferences implements Cloneable, Serializable {

  /**
   * This is the maximum duration for access/egress street searches. This is a performance limit and
   * should therefore be set high. Results close to the limit are not guaranteed to be optimal. Use
   * filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  // TODO: 2022-08-25 why do we have this field if it's never used?
  private Duration maxAccessEgressDuration = Duration.ofMinutes(45);
  /**
   * Override the settings in maxAccessEgressDuration for specific street modes. This is done
   * because some street modes searches are much more resource intensive than others.
   */
  private Map<StreetMode, Duration> maxAccessEgressDurationForMode = new HashMap<>();
  /**
   * This is the maximum duration for a direct street search. This is a performance limit and should
   * therefore be set high. Results close to the limit are not guaranteed to be optimal. Use filters
   * to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  private Duration maxDirectDuration = Duration.ofHours(4);
  /**
   * Override the settings in maxDirectStreetDuration for specific street modes. This is done
   * because some street modes searches are much more resource intensive than others.
   */
  private Map<StreetMode, Duration> maxDirectDurationForMode = new HashMap<>();
  /** Multiplicative factor on expected turning time. */
  private double turnReluctance = 1.0;
  /**
   * How long does it take to  an elevator, on average (actually, it probably should be a bit *more*
   * than average, to prevent optimistic trips)? Setting it to "seems like forever," while accurate,
   * will probably prevent OTP from working correctly.
   */
  // TODO: how long does it /really/ take to  an elevator?
  private int elevatorBoardTime = 90;
  /** What is the cost of boarding an elevator? */
  private int elevatorBoardCost = 90;
  /** How long does it take to advance one floor on an elevator? */
  private int elevatorHopTime = 20;

  // it is assumed that getting off an elevator is completely free
  /** What is the cost of travelling one floor on an elevator? */
  private int elevatorHopCost = 20;

  /**
   * Which path comparator to use
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
   */
  @Deprecated
  private String pathComparator = null;

  public StreetPreferences clone() {
    try {
      var clone = (StreetPreferences) super.clone();

      clone.maxAccessEgressDurationForMode = new HashMap<>(maxAccessEgressDurationForMode);
      clone.maxDirectDurationForMode = new HashMap<>(maxDirectDurationForMode);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public Duration maxDirectDuration(StreetMode mode) {
    return maxDirectDurationForMode.getOrDefault(mode, maxDirectDuration);
  }

  public Duration maxAccessEgressDuration(StreetMode mode) {
    return maxAccessEgressDurationForMode.getOrDefault(mode, maxAccessEgressDuration);
  }

  // TODO: 2022-08-22 do we want to have this method here?
  public Comparator<GraphPath> pathComparator(boolean compareStartTimes) {
    if ("duration".equals(pathComparator)) {
      return new DurationComparator();
    }
    return new PathComparator(compareStartTimes);
  }

  public String pathComparator() {
    return pathComparator;
  }

  public void setMaxAccessEgressDuration(Duration maxAccessEgressDuration) {
    this.maxAccessEgressDuration = maxAccessEgressDuration;
  }

  public void setMaxAccessEgressDurationForMode(
    Map<StreetMode, Duration> maxAccessEgressDurationForMode
  ) {
    this.maxAccessEgressDurationForMode = maxAccessEgressDurationForMode;
  }

  public void setMaxDirectDuration(Duration maxDirectDuration) {
    this.maxDirectDuration = maxDirectDuration;
  }

  public Duration maxDirectDuration() {
    return maxDirectDuration;
  }

  public void setMaxDirectDurationForMode(Map<StreetMode, Duration> maxDirectDurationForMode) {
    this.maxDirectDurationForMode = maxDirectDurationForMode;
  }

  public Map<StreetMode, Duration> maxDirectDurationForMode() {
    return maxDirectDurationForMode;
  }

  public void setTurnReluctance(double turnReluctance) {
    this.turnReluctance = turnReluctance;
  }

  public double turnReluctance() {
    return turnReluctance;
  }

  public void setElevatorBoardTime(int elevatorBoardTime) {
    this.elevatorBoardTime = elevatorBoardTime;
  }

  public int elevatorBoardTime() {
    return elevatorBoardTime;
  }

  public void setElevatorBoardCost(int elevatorBoardCost) {
    this.elevatorBoardCost = elevatorBoardCost;
  }

  public int elevatorBoardCost() {
    return elevatorBoardCost;
  }

  public void setElevatorHopTime(int elevatorHopTime) {
    this.elevatorHopTime = elevatorHopTime;
  }

  public int elevatorHopTime() {
    return elevatorHopTime;
  }

  public void setPathComparator(String pathComparator) {
    this.pathComparator = pathComparator;
  }

  public void setElevatorHopCost(int elevatorHopCost) {
    this.elevatorHopCost = elevatorHopCost;
  }

  public int elevatorHopCost() {
    return elevatorHopCost;
  }
}

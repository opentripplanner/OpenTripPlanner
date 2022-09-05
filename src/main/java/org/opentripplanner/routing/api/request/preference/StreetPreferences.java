package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.impl.DurationComparator;
import org.opentripplanner.routing.impl.PathComparator;
import org.opentripplanner.routing.spt.GraphPath;

// TODO VIA: Javadoc
// Direct street search
public class StreetPreferences implements Cloneable, Serializable {

  // TODO VIA: Remove direct/access/egress references and map things into multiple StreetPreferences, one per type

  private int elevatorBoardCost = 90;
  // TODO: how long does it /really/ take to  an elevator?
  private int elevatorBoardTime = 90;
  private int elevatorHopTime = 20;

  private int elevatorHopCost = 20;

  private DurationForEnum<StreetMode> maxAccessEgressDuration = new DurationForEnum<>(
    StreetMode.class,
    Duration.ofMinutes(45)
  );

  private DurationForEnum<StreetMode> maxDirectDuration = new DurationForEnum<>(
    StreetMode.class,
    Duration.ofHours(4)
  );

  private double turnReluctance = 1.0;

  /**
   * Which path comparator to use
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
   */
  @Deprecated
  private String pathComparator = null;

  /** What is the cost of boarding an elevator? */
  public int elevatorBoardCost() {
    return elevatorBoardCost;
  }

  public void setElevatorBoardCost(int elevatorBoardCost) {
    this.elevatorBoardCost = elevatorBoardCost;
  }

  /**
   * How long does it take to  an elevator, on average (actually, it probably should be a bit *more*
   * than average, to prevent optimistic trips)? Setting it to "seems like forever," while accurate,
   * will probably prevent OTP from working correctly.
   */
  public int elevatorBoardTime() {
    return elevatorBoardTime;
  }

  public void setElevatorBoardTime(int elevatorBoardTime) {
    this.elevatorBoardTime = elevatorBoardTime;
  }

  /** How long does it take to advance one floor on an elevator? */
  public int elevatorHopTime() {
    return elevatorHopTime;
  }

  public void setElevatorHopTime(int elevatorHopTime) {
    this.elevatorHopTime = elevatorHopTime;
  }

  /**
   * What is the cost of travelling one floor on an elevator?
   * It is assumed that getting off an elevator is completely free.
   * */
  public int elevatorHopCost() {
    return elevatorHopCost;
  }

  public void setElevatorHopCost(int elevatorHopCost) {
    this.elevatorHopCost = elevatorHopCost;
  }

  /** @see #maxAccessEgressDuration(StreetMode) */
  public Duration maxAccessEgressDurationDefaultValue() {
    return maxAccessEgressDuration.defaultValue();
  }

  /**
   * This is the maximum duration for access/egress per street mode for street searches. This is a
   * performance limit and should therefore be set high. Results close to the limit are not
   * guaranteed to be optimal. Use* itinerary-filters to limit what is presented to the client.
   * <p>
   * The duration can be set per mode, because some street modes searches are much more resource
   * intensive than others.
   */
  public Duration maxAccessEgressDuration(StreetMode mode) {
    return maxAccessEgressDuration.valueOf(mode);
  }

  public void initMaxAccessEgressDuration(
    Duration defaultValue,
    Map<StreetMode, Duration> valuePerMode
  ) {
    this.maxAccessEgressDuration =
      new DurationForEnum<>(StreetMode.class, defaultValue, valuePerMode);
  }

  /**
   * This is the maximum duration for a direct street search for each mode. This is a performance
   * limit and should therefore be set high. Results close to the limit are not guaranteed to be
   * optimal. Use itinerary-filters to limit what is presented to the client.
   * <p>
   * The duration can be set per mode, because some street modes searches are much more resource
   * intensive than others.
   */
  public Duration maxDirectDurationDefaultValue() {
    return maxDirectDuration.defaultValue();
  }

  public Duration maxDirectDuration(StreetMode mode) {
    return maxDirectDuration.valueOf(mode);
  }

  public void initMaxDirectDuration(Duration defaultValue, Map<StreetMode, Duration> valuePerMode) {
    this.maxDirectDuration = new DurationForEnum<>(StreetMode.class, defaultValue, valuePerMode);
  }

  /** Multiplicative factor on expected turning time. */
  public double turnReluctance() {
    return turnReluctance;
  }

  public void setTurnReluctance(double turnReluctance) {
    this.turnReluctance = turnReluctance;
  }

  // TODO VIA: 2022-08-22 do we want to have this method here?
  public String pathComparator() {
    return pathComparator;
  }

  public Comparator<GraphPath> pathComparator(boolean compareStartTimes) {
    if ("duration".equals(pathComparator)) {
      return new DurationComparator();
    }
    return new PathComparator(compareStartTimes);
  }

  public void setPathComparator(String pathComparator) {
    this.pathComparator = pathComparator;
  }

  public StreetPreferences clone() {
    try {
      var clone = (StreetPreferences) super.clone();

      clone.maxAccessEgressDuration = this.maxAccessEgressDuration;

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}

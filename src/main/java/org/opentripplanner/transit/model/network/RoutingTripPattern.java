package org.opentripplanner.transit.model.network;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class RoutingTripPattern implements Serializable {

  private static final AtomicInteger INDEX_COUNTER = new AtomicInteger(0);
  private final int index;
  private final TripPattern pattern;
  private final int[] stopIndexes;
  private final BitSet boardingPossible;
  private final BitSet alightingPossible;
  private final BitSet wheelchairAccessible;

  RoutingTripPattern(TripPattern pattern) {
    this.pattern = pattern;
    this.stopIndexes = pattern.getStops().stream().mapToInt(StopLocation::getIndex).toArray();
    this.index = INDEX_COUNTER.getAndIncrement();

    final int nStops = stopIndexes.length;
    boardingPossible = new BitSet(nStops);
    alightingPossible = new BitSet(nStops);
    wheelchairAccessible = new BitSet(nStops);

    for (int s = 0; s < nStops; s++) {
      boardingPossible.set(s, pattern.canBoard(s));
      alightingPossible.set(s, pattern.canAlight(s));
      wheelchairAccessible.set(s, pattern.wheelchairAccessible(s));
    }
  }

  public FeedScopedId getId() {
    return pattern.getId();
  }

  /**
   * This is the OTP internal <em>synthetic key</em>, used to reference a StopLocation inside OTP.  This is used
   * to optimize routing, we do not access the stop instance only keep the {code index}. The index will not change.
   * <p>
   * Do NOT expose this index in the APIs, it is not guaranteed to be the same across different OTP instances,
   * use the {code id} for external references.
   */
  public int getIndex() {
    return index;
  }

  public TransitMode getTransitMode() {
    return pattern.getMode();
  }

  public int[] getStopIndexes() {
    return stopIndexes;
  }

  public final TripPattern getPattern() {
    return this.pattern;
  }

  public BitSet getBoardingPossible() {
    return boardingPossible;
  }

  public BitSet getAlightingPossible() {
    return alightingPossible;
  }

  public BitSet getWheelchairAccessible() {
    return wheelchairAccessible;
  }

  /**
   * See {@link org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern#stopIndex(int)}
   */
  public int stopIndex(int stopPositionInPattern) {
    return stopIndexes[stopPositionInPattern];
  }

  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return boardingPossible.get(stopPositionInPattern);
  }

  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return alightingPossible.get(stopPositionInPattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoutingTripPattern that = (RoutingTripPattern) o;
    return getId() == that.getId();
  }

  @Override
  public String toString() {
    return "TripPattern{" + "id=" + getId() + ", transitMode=" + pattern.getMode() + '}';
  }

  public static int indexCounter() {
    return INDEX_COUNTER.get();
  }

  /**
   * Use this ONLY when deserializing the graph. Sets the counter value to the highest recorded value
   */
  public static void initIndexCounter(int indexCounter) {
    INDEX_COUNTER.set(indexCounter);
  }
}

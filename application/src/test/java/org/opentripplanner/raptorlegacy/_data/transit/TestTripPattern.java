package org.opentripplanner.raptorlegacy._data.transit;

import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class TestTripPattern implements DefaultTripPattern {

  public static final byte BOARDING_MASK = 0b0001;
  public static final byte ALIGHTING_MASK = 0b0010;
  public static final byte WHEELCHAIR_MASK = 0b0100;

  private final String name;
  private final int[] stopIndexes;
  /**
   * By caching the index, we avoid looking up the pattern during routing, this reduces memory lookups and
   * improves the performance.
   */
  private int slackIndex = 0;

  private int patternIndex = 0;

  private int priorityGroupId = 0;

  /**
   * <pre>
   * 0 - 000 : No restriction
   * 1 - 001 : No Boarding
   * 2 - 010 : No Alighting
   * 4 - 100 : No wheelchair
   * </pre>
   */
  private final int[] restrictions;

  private Route route;

  private TestTripPattern(String name, int[] stopIndexes, int[] restrictions) {
    this.name = name;
    this.stopIndexes = stopIndexes;
    this.restrictions = restrictions;
  }

  public static TestTripPattern pattern(String name, int... stopIndexes) {
    return new TestTripPattern(name, stopIndexes, new int[stopIndexes.length]);
  }

  /** Create a pattern with name 'R1' and given stop indexes */
  public static TestTripPattern pattern(int... stopIndexes) {
    return new TestTripPattern("R1", stopIndexes, new int[stopIndexes.length]);
  }

  public TestTripPattern withSlackIndex(int index) {
    this.slackIndex = index;
    return this;
  }

  TestTripPattern withPatternIndex(int index) {
    this.patternIndex = index;
    return this;
  }

  public TestTripPattern withRoute(Route route) {
    this.route = route;
    return this;
  }

  /**
   * <pre>
   * Codes:
   *   B : Board
   *   A : Alight
   *   W : Wheelchair
   *   * : Board, Alight, Wheelchair
   *
   * Example:   B BA * AW
   * </pre>
   */
  public void restrictions(String codes) {
    String[] split = codes.split(" ");
    for (int i = 0; i < split.length; i++) {
      String restriction = split[i];
      restrictions[i] = 0;
      if (restriction.contains("*")) {
        continue;
      }
      if (!restriction.contains("B")) {
        restrictions[i] |= BOARDING_MASK;
      }
      if (!restriction.contains("A")) {
        restrictions[i] |= ALIGHTING_MASK;
      }
      if (!restriction.contains("W")) {
        restrictions[i] |= WHEELCHAIR_MASK;
      }
    }
  }

  @Override
  public int stopIndex(int stopPositionInPattern) {
    return stopIndexes[stopPositionInPattern];
  }

  @Override
  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return isNotRestricted(stopPositionInPattern, BOARDING_MASK);
  }

  @Override
  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return isNotRestricted(stopPositionInPattern, ALIGHTING_MASK);
  }

  @Override
  public int slackIndex() {
    return slackIndex;
  }

  @Override
  public int priorityGroupId() {
    return priorityGroupId;
  }

  @Override
  public int patternIndex() {
    return patternIndex;
  }

  @Override
  public int numberOfStopsInPattern() {
    return stopIndexes.length;
  }

  @Override
  public Route route() {
    return route;
  }

  @Override
  public String debugInfo() {
    return "BUS " + name;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TestTripPattern.class)
      .addStr("name", name)
      .addInts("stops", stopIndexes)
      .addInts("restrictions", restrictions)
      .toString();
  }

  private boolean isNotRestricted(int index, int mask) {
    return restrictions == null || (restrictions[index] & mask) == 0;
  }
}

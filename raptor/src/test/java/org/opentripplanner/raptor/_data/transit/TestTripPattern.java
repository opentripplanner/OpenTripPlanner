package org.opentripplanner.raptor._data.transit;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class TestTripPattern implements RaptorTripPattern {

  private final String name;
  private final int[] stopIndexes;
  private final BoardAlightRestrictions restrictions;
  private final int slackIndex;
  private final int patternIndex;
  private final int priorityGroupId;

  private TestTripPattern(
    String name,
    int[] stopIndexes,
    BoardAlightRestrictions restrictions,
    int slackIndex,
    int patternIndex,
    int priorityGroupId
  ) {
    this.name = Objects.requireNonNull(name);
    this.stopIndexes = Objects.requireNonNull(stopIndexes);
    this.restrictions = Objects.requireNonNull(restrictions);
    this.slackIndex = slackIndex;
    this.patternIndex = patternIndex;
    this.priorityGroupId = priorityGroupId;
  }

  public static TestTripPattern.Builder of(String name, int... stopIndexes) {
    return new TestTripPattern.Builder(name, stopIndexes);
  }

  public static TestTripPattern pattern(String name, int... stopIndexes) {
    return of(name, stopIndexes).build();
  }

  /** Create a pattern with name 'R1' and given stop indexes */
  public static TestTripPattern pattern(int... stopIndexes) {
    return pattern("R1", stopIndexes);
  }

  @Override
  public int stopIndex(int stopPositionInPattern) {
    return stopIndexes[stopPositionInPattern];
  }

  @Override
  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return restrictions.isBoardingPossibleAt(stopPositionInPattern);
  }

  @Override
  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return restrictions.isAlightingPossibleAt(stopPositionInPattern);
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
  public String debugInfo() {
    return "BUS " + name;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TestTripPattern.class)
      .addStr("name", name)
      .addInts("stops", stopIndexes)
      .addObj("restrictions", restrictions)
      .toString();
  }

  public static class Builder {

    private final String name;
    private int[] stopIndexes;
    private String restrictions;
    private int slackIndex = 0;
    private int patternIndex = 0;
    private int priorityGroupId = 0;

    public Builder(String name, int... stopIndexes) {
      this.name = name;
      this.stopIndexes = stopIndexes;
    }

    public Builder pattern(int... stopIndexes) {
      this.stopIndexes = stopIndexes;
      return this;
    }

    /**
     * Set alight and board restriction using a "coded" string, use space as a separator
     * between stops.
     * <pre>
     * Codes:
     *   b : Board
     *   a : Alight
     *   * : Board & Alight
     *   - : Boarding & Alighting is not allowed
     *
     * Example:   B BA * A
     * </pre>
     */
    public Builder restrictions(String restrictions) {
      this.restrictions = restrictions;
      return this;
    }

    public Builder slackIndex(int index) {
      this.slackIndex = index;
      return this;
    }

    public Builder patternIndex(int index) {
      this.patternIndex = index;
      return this;
    }

    public Builder priorityGroup(int priorityGroupId) {
      this.priorityGroupId = priorityGroupId;
      return this;
    }

    public TestTripPattern build() {
      return new TestTripPattern(
        name,
        stopIndexes,
        restrictions == null
          ? BoardAlightRestrictions.noRestriction(stopIndexes.length)
          : BoardAlightRestrictions.restrictions(restrictions),
        slackIndex,
        patternIndex,
        priorityGroupId
      );
    }
  }
}

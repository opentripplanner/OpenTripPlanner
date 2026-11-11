package org.opentripplanner.transit.model.basic;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * This is used as a data container for when we want more accurate mode matching than is available
 * with just MainAndSubMode. Currently the additional filtering of replacement and distance
 * category is implemented, but this is designed to be capable of holding whatever NeTEx submode/GTFS
 * extended type derived features we implement.
 * <p>
 * Use {@link #of(TransitMode)} to create an instance; all narrowing features are optional and
 * default to "ignored".
 *
 * @see org.opentripplanner.model.modes.AllowNarrowedTransitModeFilter
 */
public final class NarrowedTransitMode {

  private static final List<NarrowedTransitMode> ALL = Stream.of(TransitMode.values())
    .map(mode -> of(mode).build())
    .toList();

  private final TransitMode mode;

  @Nullable
  private final SubMode subMode;

  private final ReplacementRequirement replacement;

  @Nullable
  private final DistanceCategory distanceCategory;

  private NarrowedTransitMode(Builder builder) {
    this.mode = builder.mode;
    this.subMode = builder.subMode;
    this.replacement = builder.replacement;
    this.distanceCategory = builder.distanceCategory;
  }

  /**
   * Create a builder for a narrowed mode with the given main mode. Without further narrowing, the
   * result matches all trips of the main mode.
   */
  public static Builder of(TransitMode mode) {
    return new Builder(mode);
  }

  public static NarrowedTransitMode ofMainAndSubMode(MainAndSubMode mode) {
    return of(mode.mainMode()).withSubMode(mode.subMode()).build();
  }

  public static List<NarrowedTransitMode> all() {
    return ALL;
  }

  public boolean isMainModeOnly() {
    return this.subMode == null && isConvertibleToMainAndSubMode();
  }

  /**
   * Whether this only narrows the mode by the main mode and submode, so it can be matched with the
   * faster {@link MainAndSubMode} based filters.
   */
  public boolean isConvertibleToMainAndSubMode() {
    return this.replacement == ReplacementRequirement.IGNORED && this.distanceCategory == null;
  }

  public MainAndSubMode toMainAndSubMode() {
    if (!isConvertibleToMainAndSubMode()) {
      throw new IllegalArgumentException("Not convertible to MainAndSubMode");
    }
    return new MainAndSubMode(this.mode, this.subMode);
  }

  public TransitMode getMode() {
    return mode;
  }

  /**
   * null here means that we don't care about what SubMode the trip has
   */
  @Nullable
  public SubMode getSubMode() {
    return subMode;
  }

  public ReplacementRequirement getReplacement() {
    return replacement;
  }

  /**
   * null here means that we don't care about the distance category of the trip
   */
  @Nullable
  public DistanceCategory getDistanceCategory() {
    return distanceCategory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NarrowedTransitMode that = (NarrowedTransitMode) o;
    return (
      mode == that.mode &&
      Objects.equals(subMode, that.subMode) &&
      replacement == that.replacement &&
      distanceCategory == that.distanceCategory
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode, subMode, replacement, distanceCategory);
  }

  @Override
  public String toString() {
    if (distanceCategory != null) {
      return (
        mode.name() +
        "::" +
        (subMode == null ? "" : subMode.name()) +
        "::" +
        replacement +
        "::" +
        distanceCategory
      );
    }
    if (replacement != ReplacementRequirement.IGNORED) {
      return mode.name() + "::" + (subMode == null ? "" : subMode.name()) + "::" + replacement;
    }
    if (subMode == null) {
      return mode.name();
    }
    return mode.name() + "::" + subMode;
  }

  /**
   * Make sure the String serialization is deterministic by sorting the elements in alphabetic
   * order.
   *
   * @see MainAndSubMode#toString(Collection)
   */
  public static String toString(Collection<NarrowedTransitMode> modes) {
    return modes != null
      ? modes.stream().map(NarrowedTransitMode::toString).sorted().toList().toString()
      : null;
  }

  public static class Builder {

    private final TransitMode mode;

    @Nullable
    private SubMode subMode;

    private ReplacementRequirement replacement = ReplacementRequirement.IGNORED;

    @Nullable
    private DistanceCategory distanceCategory;

    private Builder(TransitMode mode) {
      this.mode = Objects.requireNonNull(mode);
    }

    /**
     * Only match trips with the given submode, or any submode if null.
     */
    public Builder withSubMode(@Nullable SubMode subMode) {
      this.subMode = subMode;
      return this;
    }

    public Builder withReplacement(ReplacementRequirement replacement) {
      this.replacement = Objects.requireNonNull(replacement);
      return this;
    }

    /**
     * Only match trips with the given distance category, or any category if null.
     */
    public Builder withDistanceCategory(@Nullable DistanceCategory distanceCategory) {
      this.distanceCategory = distanceCategory;
      return this;
    }

    public NarrowedTransitMode build() {
      return new NarrowedTransitMode(this);
    }
  }
}

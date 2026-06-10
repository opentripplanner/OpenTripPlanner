package org.opentripplanner.transit.model.basic;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * This is used as a data container for when we want more accurate mode matching than is available
 * with just MainAndSubMode. Currently only the additional filtering of replacement is implemented,
 * but this is designed to be capable of holding whatever NeTEx submode/GTFS extended type derived
 * features we implement.
 *
 * @see org.opentripplanner.model.modes.AllowNarrowedTransitModeFilter
 */
public class NarrowedTransitMode {

  private static final List<NarrowedTransitMode> ALL = Stream.of(TransitMode.values())
    .map(mode -> new NarrowedTransitMode(mode, null, ReplacementRequirement.IGNORED))
    .toList();

  private TransitMode mode;

  @Nullable
  private SubMode subMode;

  private ReplacementRequirement replacement;

  public NarrowedTransitMode(
    TransitMode mode,
    @Nullable SubMode subMode,
    ReplacementRequirement replacement
  ) {
    this.mode = mode;
    this.subMode = subMode;
    this.replacement = replacement;
  }

  public static NarrowedTransitMode of(MainAndSubMode mode) {
    return new NarrowedTransitMode(mode.mainMode(), mode.subMode(), ReplacementRequirement.IGNORED);
  }

  public static List<NarrowedTransitMode> all() {
    return ALL;
  }

  public boolean isMainModeOnly() {
    return (this.subMode == null && this.replacement == ReplacementRequirement.IGNORED);
  }

  public MainAndSubMode toMainAndSubMode() {
    if (this.replacement != ReplacementRequirement.IGNORED) {
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

  public String toString() {
    if (replacement != ReplacementRequirement.IGNORED) {
      return mode.name() + "::" + (subMode == null ? "" : subMode.name()) + "::" + replacement;
    }
    if (subMode == null) {
      return mode.name();
    }
    return mode.name() + "::" + subMode;
  }

  /**
   * Make sure the String serialization is deterministic by sorting the elements in
   * alphabetic order.
   *
   * @see MainAndSubMode#toString(Collection)
   */
  public static String toString(Collection<NarrowedTransitMode> modes) {
    return modes != null
      ? modes.stream().map(NarrowedTransitMode::toString).sorted().toList().toString()
      : null;
  }
}

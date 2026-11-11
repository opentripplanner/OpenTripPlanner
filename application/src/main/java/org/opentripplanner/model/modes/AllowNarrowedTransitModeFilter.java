package org.opentripplanner.model.modes;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.DistanceCategory;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.ReplacementRequirement;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.ReplacementHelper;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class AllowNarrowedTransitModeFilter implements AllowTransitModeFilter {

  private final NarrowedTransitMode mode;

  public AllowNarrowedTransitModeFilter(NarrowedTransitMode mode) {
    this.mode = mode;
  }

  @Override
  public boolean isModeSelective() {
    return true;
  }

  TransitMode mainMode() {
    return this.mode.getMode();
  }

  @Override
  public boolean match(
    TransitMode transitMode,
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    if (mode.getMode() != transitMode) {
      return false;
    }
    // The distance category is an additional requirement: a trip must always have the requested
    // category, no matter how the submode and replacement conditions below are evaluated.
    if (!distanceCategoryMatches(transitMode, netexSubmode, gtfsExtendedType)) {
      return false;
    }
    // Three-valued boolean logic here. If more dimensions are added to NarrowedTransitMode
    // (like express/local trains etc), they can be implemented symmetrically.
    // See truth table:
    //                     replacementMatches
    //                     :     replacementIgnored
    //                   | T  F  x
    //                 --+---------
    //  subModeMatches T | T  T  T
    //                 F | T  F  F
    //  subModeIgnored x | T  F  T
    var subModeIgnored = mode.getSubMode() == null;
    var replacementIgnored = mode.getReplacement() == ReplacementRequirement.IGNORED;
    if (subModeIgnored && replacementIgnored) {
      return true;
    }
    var subModeMatches = netexSubmode.equals(mode.getSubMode());
    var isTripReplacement = isTripReplacement(netexSubmode, gtfsExtendedType);
    var replacementMatches = mode.getReplacement() == mapReplacement(isTripReplacement);
    return subModeMatches || replacementMatches;
  }

  private boolean distanceCategoryMatches(
    TransitMode transitMode,
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    var category = mode.getDistanceCategory();
    return (
      category == null ||
      DistanceCategory.of(transitMode, netexSubmode, gtfsExtendedType) == category
    );
  }

  private static ReplacementRequirement mapReplacement(boolean replacement) {
    return replacement ? ReplacementRequirement.REQUIRED : ReplacementRequirement.FORBIDDEN;
  }

  /**
   * This value is currently calculated at query time, but we are moving toward having it constantly
   * up to date in the model. That will enable us to do much faster selection of matching trips
   * and/or patterns with bitfields and logical operations, at which point this entire filter
   * mechanism will be removed as unnecessary. This will also simplify the IGNORED logic by quite a
   * bit.
   */
  private static boolean isTripReplacement(
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    return ReplacementHelper.isReplacement(netexSubmode, gtfsExtendedType);
  }

  @Override
  public int hashCode() {
    return mode.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllowNarrowedTransitModeFilter that = (AllowNarrowedTransitModeFilter) o;
    return mode.equals(that.mode);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AllowNarrowedTransitModeFilter.class)
      .addEnum("mode", mode.getMode())
      .addObj("subMode", mode.getSubMode())
      .addEnum("isReplacement", mode.getReplacement())
      .addEnum("distanceCategory", mode.getDistanceCategory())
      .toString();
  }
}

package org.opentripplanner.model.modes;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.ReplacementRequirement;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.ReplacementHelper;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class AllowNarrowedTransitModeFilter implements AllowTransitModeFilter {

  NarrowedTransitMode mode;

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

  private static ReplacementRequirement mapReplacement(boolean replacement) {
    return replacement ? ReplacementRequirement.REQUIRED : ReplacementRequirement.FORBIDDEN;
  }

  /**
   * This value is currently calculated at query time, but we are moving toward having it constantly
   * up to date in the model. That will enable us to do much faster selection of matching trips and/or
   * patterns with bitfields and logical operations, at which point this entire filter mechanism
   * will be removed as unnecessary. This will also simplify the IGNORED logic by quite a bit.
   */
  private static boolean isTripReplacement(
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    return ReplacementHelper.isReplacement(netexSubmode, gtfsExtendedType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode.getMode(), mode.getSubMode(), mode.getReplacement());
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
    return (
      Objects.equals(mode.getMode(), that.mode.getMode()) &&
      Objects.equals(mode.getSubMode(), that.mode.getSubMode()) &&
      Objects.equals(mode.getReplacement(), that.mode.getReplacement())
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AllowNarrowedTransitModeFilter.class)
      .addEnum("mode", mode.getMode())
      .addObj("subMode", mode.getSubMode())
      .addEnum("isReplacement", mode.getReplacement())
      .toString();
  }
}

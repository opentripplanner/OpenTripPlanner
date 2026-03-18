package org.opentripplanner.netex.mapping;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.netex.config.SwissProfile;
import org.rutebanken.netex.model.DestinationDisplay;

/**
 * Maps Netex DestinationDisplay to I18NString.
 */
class HeadsignMapper {

  @Nullable
  static I18NString mapHeadsign(DestinationDisplay destinationDisplay) {
    if (destinationDisplay.getFrontText() != null) {
      return I18NString.of(destinationDisplay.getFrontText().getValue());
    }

    // Swiss profile
    return ofName(destinationDisplay).orElse(null);
  }

  @SwissProfile
  private static Optional<I18NString> ofName(DestinationDisplay destinationDisplay) {
    return Optional.ofNullable(destinationDisplay.getName()).map(n -> I18NString.of(n.getValue()));
  }
}

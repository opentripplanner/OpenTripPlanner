package org.opentripplanner.netex.mapping;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.rutebanken.netex.model.DestinationDisplay;

/**
 * Maps Netex DestinationDisplay to I18NString.
 */
class HeadsignMapper {

  @Nullable
  static I18NString mapHeadsign(DestinationDisplay destinationDisplay) {
    return Optional.ofNullable(destinationDisplay.getFrontText())
      .or(() -> Optional.ofNullable(destinationDisplay.getName()))
      .map(s -> I18NString.of(s.getValue()))
      .orElse(null);
  }
}

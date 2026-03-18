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
    if(destinationDisplay.getFrontText() != null) {
      return I18NString.of(s.getValue());
    }
    // Swiss profile
    if(destinationDisplay.getName() != null) {
      return I18NString.of(s.getValue());
    }
    return null;
  }
}

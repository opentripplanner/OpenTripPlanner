package org.opentripplanner.netex.mapping;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.config.SwissProfile;
import org.rutebanken.netex.model.DestinationDisplay;

/**
 * Maps Netex DestinationDisplay to I18NString.
 */
class HeadsignMapper {

  private final DataImportIssueStore issueStore;

  HeadsignMapper(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  @Nullable
  I18NString map(DestinationDisplay destinationDisplay) {
    if (destinationDisplay.getFrontText() != null) {
      return I18NString.of(destinationDisplay.getFrontText().getValue());
    }

    // Swiss profile
    var res = ofName(destinationDisplay).orElse(null);

    if (res == null) {
      issueStore.add(
        "EmptyDestinationDisplay",
        "DestinationDisplay contains no usable values %s",
        destinationDisplay
      );
    }
    return res;
  }

  @SwissProfile
  private static Optional<I18NString> ofName(DestinationDisplay destinationDisplay) {
    return Optional.ofNullable(destinationDisplay.getName()).map(n -> I18NString.of(n.getValue()));
  }
}

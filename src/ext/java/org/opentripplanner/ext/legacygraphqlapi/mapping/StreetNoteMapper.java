package org.opentripplanner.ext.legacygraphqlapi.mapping;

import org.opentripplanner.api.mapping.StreetNoteMaperMapper;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class StreetNoteMapper {

  /**
   * Similar to {@link StreetNoteMaperMapper ::mapToApi}.
   */
  public static TransitAlert mapStreetNoteToAlert(StreetNote note) {
    // TODO: The ID is used only in the mapping, we should instead have two mappers for the fields
    TransitAlertBuilder alert = TransitAlert.of(
      new FeedScopedId("StreetNote", Integer.toString(note.hashCode()))
    );
    alert.withHeaderText(note.note);
    alert.withDescriptionText(note.descriptionText);
    alert.withUrl(NonLocalizedString.ofNullable(note.url));
    if (note.effectiveStartDate != null && note.effectiveEndDate != null) {
      alert.addTimePeriod(
        new TimePeriod(
          note.effectiveStartDate.getTime() / 1000,
          note.effectiveEndDate.getTime() / 1000
        )
      );
    }
    return alert.build();
  }
}

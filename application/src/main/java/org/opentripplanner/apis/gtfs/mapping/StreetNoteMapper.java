package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.alertpatch.TransitAlertBuilder;
import org.opentripplanner.street.model.note.StreetNote;

public class StreetNoteMapper {

  public static TransitAlert mapStreetNoteToAlert(StreetNote note) {
    // TODO: The ID is used only in the mapping, we should instead have two mappers for the fields
    TransitAlertBuilder alert = TransitAlert.of(
      new FeedScopedId("StreetNote", Integer.toString(note.hashCode()))
    );
    alert.withHeaderText(note.note);
    alert.withDescriptionText(note.descriptionText);
    alert.withUrl(NonLocalizedString.ofNullable(note.url));
    if (note.effectiveStartDate != null && note.effectiveEndDate != null) {
      alert.withCalendar(
        AlertCalendar.of(
          TimePeriod.of(note.effectiveStartDate.toInstant(), note.effectiveEndDate.toInstant())
        )
      );
    }
    return alert.build();
  }
}

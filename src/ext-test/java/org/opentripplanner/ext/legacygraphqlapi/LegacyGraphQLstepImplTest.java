package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.legacygraphqlapi.datafetchers.LegacyGraphQLstepImpl;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.note.StreetNote;

class LegacyGraphQLstepImplTest {

  public static final String TEST_STREET_NOTE_HEADER = "Test Street Note";
  public static final String TEST_STREET_NOTE_DESCRIPTION = "Test Street Note Description";
  public static final String TEST_STREET_NOTE_URL = "http://www.example.com/test-note";

  @Test
  void testMapStreetNoteToAlert() {
    StreetNote note = new StreetNote(TEST_STREET_NOTE_HEADER);
    note.descriptionText = locale -> TEST_STREET_NOTE_DESCRIPTION;
    note.url = TEST_STREET_NOTE_URL;
    Date startDate = new Date();
    note.effectiveStartDate = startDate;
    // Truncate instant to seconds because {@link TimePeriod::new} takes seconds, not milliseconds.
    Instant startInstant = startDate.toInstant().truncatedTo(ChronoUnit.SECONDS);
    Instant endInstant = startInstant.plusSeconds(3600);
    note.effectiveEndDate = Date.from(endInstant);

    TransitAlert alert = LegacyGraphQLstepImpl.mapStreetNoteToAlert(note);
    assertEquals(TEST_STREET_NOTE_HEADER, alert.alertHeaderText.toString(Locale.ROOT));
    assertEquals(TEST_STREET_NOTE_DESCRIPTION, alert.alertDescriptionText.toString(Locale.ROOT));
    assertEquals(TEST_STREET_NOTE_URL, alert.alertUrl.toString(Locale.ROOT));
    assertEquals(startInstant, alert.getEffectiveStartDate());
    assertEquals(endInstant, alert.getEffectiveEndDate());
  }
}

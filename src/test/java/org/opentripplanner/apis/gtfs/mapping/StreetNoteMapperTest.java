package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.note.StreetNote;

class StreetNoteMapperTest {

  static final String TEST_STREET_NOTE_HEADER = "Test Street Note";
  static final String TEST_STREET_NOTE_DESCRIPTION = "Test Street Note Description";
  static final String TEST_STREET_NOTE_URL = "http://www.example.com/test-note";
  static final Date START_DATE = new Date();
  static final Instant START_INSTANCE = START_DATE.toInstant().truncatedTo(ChronoUnit.SECONDS);
  static final Instant END_INSTANCE = START_INSTANCE.plusSeconds(3600);

  @Test
  void mapRegularAlert() {
    var note = note();
    TransitAlert alert = StreetNoteMapper.mapStreetNoteToAlert(note);
    assertEquals(TEST_STREET_NOTE_HEADER, alert.headerText().get().toString(Locale.ROOT));
    assertEquals(TEST_STREET_NOTE_DESCRIPTION, alert.descriptionText().get().toString(Locale.ROOT));
    assertEquals(TEST_STREET_NOTE_URL, alert.url().get().toString(Locale.ROOT));
    assertEquals(START_INSTANCE, alert.getEffectiveStartDate());
    assertEquals(END_INSTANCE, alert.getEffectiveEndDate());
  }

  @Test
  void mapNullUrl() {
    var note = note();
    note.url = null;
    TransitAlert alert = StreetNoteMapper.mapStreetNoteToAlert(note);
    assertEquals(Optional.empty(), alert.url());
  }

  @Test
  void noTime() {
    var note = note();
    note.effectiveStartDate = null;
    note.effectiveEndDate = null;
    TransitAlert alert = StreetNoteMapper.mapStreetNoteToAlert(note);
    assertEquals(Collections.emptyList(), alert.timePeriods());
  }

  private static StreetNote note() {
    StreetNote note = new StreetNote(TEST_STREET_NOTE_HEADER);
    note.descriptionText = locale -> TEST_STREET_NOTE_DESCRIPTION;
    note.url = TEST_STREET_NOTE_URL;
    note.effectiveStartDate = START_DATE;
    // Truncate instant to seconds because {@link TimePeriod::new} takes seconds, not milliseconds.
    note.effectiveEndDate = Date.from(END_INSTANCE);
    return note;
  }
}

package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class GroupOfStationsTest {

  private static TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");

  private static final Station STATION = TEST_MODEL.station("1:station").build();

  private static final StopLocation STOP_LOCATION = TEST_MODEL.stop("1:stop", 1d, 1d)
    .withParentStation(STATION)
    .build();

  private static final GroupOfStations SUBJECT = GroupOfStations.of(
    TimetableRepositoryForTest.id(ID)
  )
    .withName(NAME)
    .addChildStation(STATION)
    .build();

  @Test
  void copy() {
    assertEquals(ID, SUBJECT.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = SUBJECT.copy().withName(NAME).build();

    assertSame(SUBJECT, copy);

    // Copy and change name
    copy = SUBJECT.copy().withName(new NonLocalizedString("v2")).build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(SUBJECT, copy);
    assertEquals(SUBJECT, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals(STOP_LOCATION, copy.getChildStops().iterator().next());
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy().addChildStation(TEST_MODEL.station("2:station").build()).build()
      )
    );
  }
}

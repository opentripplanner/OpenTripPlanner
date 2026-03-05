package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class MultiModalStationTest {

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");

  private static TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Station STATION_1 = TEST_MODEL.station("1:1").build();
  private static final Station STATION_2 = TEST_MODEL.station("1:2").build();

  public static final Set<Station> CHILD_STATIONS = Set.of(STATION_1, STATION_2);
  private static final MultiModalStation SUBJECT = MultiModalStation.of(
    TimetableRepositoryForTest.id(ID)
  )
    .withName(NAME)
    .withChildStations(CHILD_STATIONS)
    .withCoordinate(new WgsCoordinate(1, 1))
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
    assertTrue(copy.getChildStations().containsAll(CHILD_STATIONS));
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withChildStations(Set.of(STATION_1)).build()));
  }
}

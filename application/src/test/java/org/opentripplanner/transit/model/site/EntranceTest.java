package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class EntranceTest {

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");
  private static final I18NString DESCRIPTION = new NonLocalizedString("description");
  private static final String CODE = "code";

  public static final WgsCoordinate COORDINATE = new WgsCoordinate(0, 0);
  private static final StopLevel LEVEL = new StopLevel("level", 0);
  private static final Accessibility WHEELCHAIR_ACCESSIBILITY = Accessibility.POSSIBLE;
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Station PARENT_STATION = TEST_MODEL.station("stationId").build();

  private static final Entrance SUBJECT = Entrance.of(TimetableRepositoryForTest.id(ID))
    .withName(NAME)
    .withDescription(DESCRIPTION)
    .withCode(CODE)
    .withCoordinate(COORDINATE)
    .withLevel(LEVEL)
    .withParentStation(PARENT_STATION)
    .withWheelchairAccessibility(WHEELCHAIR_ACCESSIBILITY)
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
    assertEquals("v2", copy.getName().toString());
    assertEquals(PARENT_STATION, copy.getParentStation());
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(CODE, copy.getCode());
    assertTrue(COORDINATE.sameLocation(copy.getCoordinate()));
    assertEquals(LEVEL.name(), copy.getLevelName());
    assertEquals(LEVEL.index(), copy.getLevelIndex());
    assertEquals(WHEELCHAIR_ACCESSIBILITY, copy.getWheelchairAccessibility());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withDescription(new NonLocalizedString("X")).build())
    );
  }
}

package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class StationTest {

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");
  private static final I18NString DESCRIPTION = new NonLocalizedString("description");
  private static final String CODE = "code";

  public static final WgsCoordinate COORDINATE = new WgsCoordinate(0, 0);
  private static final StopTransferPriority PRIORITY = StopTransferPriority.ALLOWED;
  private static final ZoneId TIMEZONE = ZoneId.of(TimetableRepositoryForTest.TIME_ZONE_ID);
  private static final I18NString URL = new NonLocalizedString("url");
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Station PARENT_STATION = TEST_MODEL.station("stationId").build();

  private static final Station SUBJECT = Station.of(TimetableRepositoryForTest.id(ID))
    .withName(NAME)
    .withDescription(DESCRIPTION)
    .withCode(CODE)
    .withCoordinate(COORDINATE)
    .withPriority(PRIORITY)
    .withTimezone(TIMEZONE)
    .withUrl(URL)
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
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(CODE, copy.getCode());
    assertTrue(COORDINATE.sameLocation(copy.getCoordinate()));
    assertEquals(PRIORITY, copy.getPriority());
    assertEquals(TIMEZONE, copy.getTimezone());
    assertEquals(URL, copy.getUrl());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withCode("X").build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withDescription(new NonLocalizedString("X")).build())
    );
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withPriority(StopTransferPriority.DISCOURAGED).build())
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withCoordinate(new WgsCoordinate(1, 1)).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withUrl(new NonLocalizedString("X")).build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy()
          .withTimezone(ZoneId.of(TimetableRepositoryForTest.OTHER_TIME_ZONE_ID))
          .build()
      )
    );
  }
}

package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.service.SiteRepository;

class AreaStopTest {

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");
  private static final I18NString DESCRIPTION = new NonLocalizedString("description");

  private static final I18NString URL = new NonLocalizedString("url");

  private static final String ZONE_ID = TimetableRepositoryForTest.TIME_ZONE_ID;

  private static final Geometry GEOMETRY = Polygons.OSLO;

  private static final WgsCoordinate COORDINATE = new WgsCoordinate(59.925, 10.7376);

  private static final AreaStop SUBJECT = areaStopBuilder().withGeometry(GEOMETRY).build();

  private static AreaStopBuilder areaStopBuilder() {
    return SiteRepository.of()
      .areaStop(TimetableRepositoryForTest.id(ID))
      .withName(NAME)
      .withDescription(DESCRIPTION)
      .withUrl(URL)
      .withZoneId(ZONE_ID);
  }

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
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(URL, copy.getUrl());
    assertEquals(ZONE_ID, copy.getFirstZoneAsString());
    assertEquals(GEOMETRY, copy.getGeometry());
    assertEquals(COORDINATE, copy.getCoordinate().roundToApproximate10m());
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withDescription(new NonLocalizedString("X")).build())
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withUrl(new NonLocalizedString("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withZoneId("X").build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withGeometry(GeometryUtils.makeLineString(0, 0, 0, 2)).build())
    );
  }
}

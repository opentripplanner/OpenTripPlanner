package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.service.StopModel;

class AreaStopTest {

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");
  private static final I18NString DESCRIPTION = new NonLocalizedString("description");

  private static final I18NString URL = new NonLocalizedString("url");

  private static final String ZONE_ID = TransitModelForTest.TIME_ZONE_ID;

  private static final LineString GEOMETRY = GeometryUtils.makeLineString(10, 0, 12, 4);

  private static final WgsCoordinate COORDINATE = new WgsCoordinate(2, 11);

  private static final AreaStop subject = StopModel
    .of()
    .areaStop(TransitModelForTest.id(ID))
    .withName(NAME)
    .withDescription(DESCRIPTION)
    .withUrl(URL)
    .withZoneId(ZONE_ID)
    .withGeometry(GEOMETRY)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withName(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withName(new NonLocalizedString("v2")).build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(subject, copy);
    assertEquals(subject, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(URL, copy.getUrl());
    assertEquals(ZONE_ID, copy.getFirstZoneAsString());
    assertEquals(GEOMETRY, copy.getGeometry());
    assertEquals(COORDINATE, copy.getCoordinate());
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TransitModelForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      subject.sameAs(subject.copy().withDescription(new NonLocalizedString("X")).build())
    );
    assertFalse(subject.sameAs(subject.copy().withUrl(new NonLocalizedString("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withZoneId("X").build()));
    assertFalse(
      subject.sameAs(subject.copy().withGeometry(GeometryUtils.makeLineString(0, 0, 0, 2)).build())
    );
  }
}

package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.service.StopModel;

class GroupStopTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");

  private static final StopLocation STOP_LOCATION = TEST_MODEL.stop("1:stop", 1d, 1d).build();
  private static final GroupStop subject = StopModel
    .of()
    .groupStop(TransitModelForTest.id(ID))
    .withName(NAME)
    .addLocation(STOP_LOCATION)
    .build();

  @Test
  void testGroupStopGeometry() {
    StopLocation stopLocation1 = TEST_MODEL.stop("1:stop", 1d, 1d).build();
    StopLocation stopLocation2 = TEST_MODEL.stop("2:stop", 2d, 2d).build();

    GroupStop groupStop = StopModel
      .of()
      .groupStop(TransitModelForTest.id(ID))
      .withName(NAME)
      .addLocation(stopLocation1)
      .addLocation(stopLocation2)
      .build();

    Geometry groupStopGeometry1 = Objects.requireNonNull(groupStop.getGeometry()).getGeometryN(0);
    assertEquals(stopLocation1.getGeometry(), groupStopGeometry1);

    Geometry groupStopGeometry2 = Objects.requireNonNull(groupStop.getGeometry()).getGeometryN(1);
    assertEquals(stopLocation2.getGeometry(), groupStopGeometry2);
  }

  @Test
  void testGroupStopEncompassingAreaGeometry() {
    Geometry encompassingAreaGeometry = GeometryUtils
      .getGeometryFactory()
      .toGeometry(new Envelope(1d, 2d, 1d, 2d));

    StopLocation stopLocation = TEST_MODEL
      .stop(
        "1:stop",
        encompassingAreaGeometry.getCentroid().getX(),
        encompassingAreaGeometry.getCentroid().getY()
      )
      .build();

    GroupStop groupStop = StopModel
      .of()
      .groupStop(TransitModelForTest.id(ID))
      .withName(NAME)
      .addLocation(stopLocation)
      .withEncompassingAreaGeometries(List.of(encompassingAreaGeometry))
      .build();

    Geometry groupStopGeometry = Objects.requireNonNull(groupStop.getGeometry()).getGeometryN(0);
    assertEquals(stopLocation.getGeometry(), groupStopGeometry);

    assertEquals(
      encompassingAreaGeometry,
      groupStop.getEncompassingAreaGeometry().orElseThrow().getGeometryN(0)
    );
  }

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
    assertEquals(STOP_LOCATION, copy.getChildLocations().iterator().next());
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TransitModelForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      subject.sameAs(
        subject.copy().addLocation(TransitModelForTest.of().stop("2:stop", 1d, 2d).build()).build()
      )
    );
  }
}

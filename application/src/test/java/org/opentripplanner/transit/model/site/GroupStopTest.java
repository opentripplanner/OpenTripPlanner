package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.service.SiteRepository;

class GroupStopTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");

  private static final StopLocation STOP_LOCATION = TEST_MODEL.stop(
    "1:stop",
    Coordinates.BERLIN.getX(),
    Coordinates.BERLIN.getY()
  ).build();
  private static final GroupStop SUBJECT = SiteRepository.of()
    .groupStop(TimetableRepositoryForTest.id(ID))
    .withName(NAME)
    .addLocation(STOP_LOCATION)
    .build();

  @Test
  void testGroupStopGeometry() {
    StopLocation stopLocation1 = TEST_MODEL.stop(
      "1:stop",
      Coordinates.BERLIN.getX(),
      Coordinates.BERLIN.getY()
    ).build();
    StopLocation stopLocation2 = TEST_MODEL.stop(
      "2:stop",
      Coordinates.HAMBURG.getX(),
      Coordinates.HAMBURG.getY()
    ).build();

    GroupStop groupStop = SiteRepository.of()
      .groupStop(TimetableRepositoryForTest.id(ID))
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
    StopLocation stopLocation = TEST_MODEL.stop(
      "1:stop",
      Coordinates.BERLIN.getX(),
      Coordinates.BERLIN.getY()
    ).build();

    GroupStop groupStop = SiteRepository.of()
      .groupStop(TimetableRepositoryForTest.id(ID))
      .withName(NAME)
      .addLocation(stopLocation)
      .withEncompassingAreaGeometries(List.of(Polygons.BERLIN))
      .build();

    Geometry groupStopGeometry = Objects.requireNonNull(groupStop.getGeometry()).getGeometryN(0);
    assertEquals(stopLocation.getGeometry(), groupStopGeometry);

    assertEquals(
      Polygons.BERLIN,
      groupStop.getEncompassingAreaGeometry().orElseThrow().getGeometryN(0)
    );
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
    assertEquals(STOP_LOCATION, copy.getChildLocations().iterator().next());
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy()
          .addLocation(TimetableRepositoryForTest.of().stop("2:stop", 1d, 2d).build())
          .build()
      )
    );
  }
}

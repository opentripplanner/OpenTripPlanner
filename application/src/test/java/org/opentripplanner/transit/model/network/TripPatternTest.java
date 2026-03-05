package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

class TripPatternTest {

  private static final String ID = "1";
  private static final String NAME = "short name";
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Route ROUTE = TimetableRepositoryForTest.route("routeId").build();
  public static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  public static final RegularStop STOP_X = TEST_MODEL.stop("X").build();
  public static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  public static final RegularStop STOP_Y = TEST_MODEL.stop("Y").build();
  public static final RegularStop STOP_C = TEST_MODEL.stop("C").build();
  private static final StopPattern STOP_PATTERN = TimetableRepositoryForTest.stopPattern(
    STOP_A,
    STOP_B,
    STOP_C
  );

  private static final List<LineString> HOP_GEOMETRIES = List.of(
    makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
    makeLineString(STOP_B.getCoordinate(), STOP_Y.getCoordinate(), STOP_C.getCoordinate())
  );

  private static final TripPattern SUBJECT = TripPattern.of(id(ID))
    .withName(NAME)
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
    .withHopGeometries(HOP_GEOMETRIES)
    .build();

  @Test
  void copy() {
    assertEquals(ID, SUBJECT.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = SUBJECT.copy().withName(NAME).build();

    assertSame(SUBJECT, copy);

    // Copy and change name
    copy = SUBJECT.copy().withName("v2").build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(SUBJECT, copy);
    assertEquals(SUBJECT, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.getName());
    assertEquals(ROUTE, copy.getRoute());
    assertEquals(STOP_PATTERN, copy.getStopPattern());
    assertEquals(SUBJECT.getHopGeometry(0), copy.getHopGeometry(0));
    assertEquals(SUBJECT.getHopGeometry(1), copy.getHopGeometry(1));
    assertEquals(HOP_GEOMETRIES.get(1), copy.getHopGeometry(1));
  }

  @Test
  void hopGeometryForReplacementPattern() {
    var pattern = TripPattern.of(id("replacement"))
      .withName("replacement")
      .withRoute(ROUTE)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_X, STOP_Y))
      .withOriginalTripPattern(SUBJECT)
      .build();

    assertEquals(HOP_GEOMETRIES.get(0), pattern.getHopGeometry(0));
    assertEquals(
      makeLineString(STOP_B.getCoordinate(), STOP_X.getCoordinate()),
      pattern.getHopGeometry(1)
    );
    assertEquals(
      makeLineString(STOP_X.getCoordinate(), STOP_Y.getCoordinate()),
      pattern.getHopGeometry(2)
    );
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName("X").build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy().withRoute(TimetableRepositoryForTest.route("anotherId").build()).build()
      )
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withMode(TransitMode.RAIL).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withStopPattern(TEST_MODEL.stopPattern(11)).build()));
  }

  @Test
  void initNameShouldThrow() {
    Assertions.assertThrows(IllegalStateException.class, () -> SUBJECT.initName("abc"));
  }

  @Test
  void shouldAddName() {
    var name = "xyz";
    var noNameYet = TripPattern.of(id(ID)).withRoute(ROUTE).withStopPattern(STOP_PATTERN).build();

    noNameYet.initName(name);

    assertEquals(name, noNameYet.getName());
  }

  @Test
  void shouldResolveMode() {
    var patternWithoutExplicitMode = TripPattern.of(id(ID))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN)
      .build();

    assertEquals(patternWithoutExplicitMode.getMode(), ROUTE.getMode());
  }

  @Test
  void containsAnyStopId() {
    assertFalse(SUBJECT.containsAnyStopId(List.of()));
    assertFalse(SUBJECT.containsAnyStopId(List.of(id("not-in-pattern"))));
    assertTrue(SUBJECT.containsAnyStopId(List.of(STOP_A.getId())));
    assertTrue(SUBJECT.containsAnyStopId(List.of(STOP_A.getId(), id("not-in-pattern"))));
  }
}

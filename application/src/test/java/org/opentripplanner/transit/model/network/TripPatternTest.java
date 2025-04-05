package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.geometry.GeometryUtils.makeLineString;
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

  private static final TripPattern subject = TripPattern.of(id(ID))
    .withName(NAME)
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
    .withHopGeometries(HOP_GEOMETRIES)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withName(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withName("v2").build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(subject, copy);
    assertEquals(subject, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.getName());
    assertEquals(ROUTE, copy.getRoute());
    assertEquals(STOP_PATTERN, copy.getStopPattern());
    assertEquals(subject.getHopGeometry(0), copy.getHopGeometry(0));
    assertEquals(subject.getHopGeometry(1), copy.getHopGeometry(1));
    assertEquals(HOP_GEOMETRIES.get(1), copy.getHopGeometry(1));
  }

  @Test
  void hopGeometryForReplacementPattern() {
    var pattern = TripPattern.of(id("replacement"))
      .withName("replacement")
      .withRoute(ROUTE)
      .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_X, STOP_Y))
      .withOriginalTripPattern(subject)
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
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withName("X").build()));
    assertFalse(
      subject.sameAs(
        subject.copy().withRoute(TimetableRepositoryForTest.route("anotherId").build()).build()
      )
    );
    assertFalse(subject.sameAs(subject.copy().withMode(TransitMode.RAIL).build()));
    assertFalse(subject.sameAs(subject.copy().withStopPattern(TEST_MODEL.stopPattern(11)).build()));
  }

  @Test
  void initNameShouldThrow() {
    Assertions.assertThrows(IllegalStateException.class, () -> subject.initName("abc"));
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
    assertFalse(subject.containsAnyStopId(List.of()));
    assertFalse(subject.containsAnyStopId(List.of(id("not-in-pattern"))));
    assertTrue(subject.containsAnyStopId(List.of(STOP_A.getId())));
    assertTrue(subject.containsAnyStopId(List.of(STOP_A.getId(), id("not-in-pattern"))));
  }
}

package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;

class TripPatternTest {

  private static final String ID = "1";
  private static final String NAME = "short name";

  private static final Route ROUTE = TransitModelForTest.route("routeId").build();
  private static final StopPattern STOP_PATTERN = TransitModelForTest.stopPattern(10);
  private static final TripPattern subject = TripPattern
    .of(TransitModelForTest.id(ID))
    .withName(NAME)
    .withRoute(ROUTE)
    .withStopPattern(STOP_PATTERN)
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
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TransitModelForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withName("X").build()));
    assertFalse(
      subject.sameAs(
        subject.copy().withRoute(TransitModelForTest.route("anotherId").build()).build()
      )
    );
    assertFalse(subject.sameAs(subject.copy().withMode(TransitMode.RAIL).build()));
    assertFalse(
      subject.sameAs(subject.copy().withStopPattern(TransitModelForTest.stopPattern(11)).build())
    );
  }

  @Test
  void initNameShouldThrow() {
    Assertions.assertThrows(IllegalStateException.class, () -> subject.initName("abc"));
  }

  @Test
  void shouldAddName() {
    var name = "xyz";
    var noNameYet = TripPattern
      .of(TransitModelForTest.id(ID))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN)
      .build();

    noNameYet.initName(name);

    assertEquals(name, noNameYet.getName());
  }

  @Test
  void shouldResolveMode() {
    var patternWithoutExplicitMode = TripPattern
      .of(TransitModelForTest.id(ID))
      .withRoute(ROUTE)
      .withStopPattern(STOP_PATTERN)
      .build();

    assertEquals(patternWithoutExplicitMode.getMode(), ROUTE.getMode());
  }
}

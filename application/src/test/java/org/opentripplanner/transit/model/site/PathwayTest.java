package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class PathwayTest {

  private static final String ID = "1:pathway";
  private static final String NAME = "name";
  private static final PathwayMode MODE = PathwayMode.ESCALATOR;
  private static final PathwayNode FROM = PathwayNode.of(TimetableRepositoryForTest.id("1:node"))
    .withCoordinate(new WgsCoordinate(20, 30))
    .build();
  private static final RegularStop TO = TimetableRepositoryForTest.of().stop("1:stop").build();
  public static final int TRAVERSAL_TIME = 120;

  private final Pathway subject = Pathway.of(TimetableRepositoryForTest.id(ID))
    .withPathwayMode(MODE)
    .withSignpostedAs(NAME)
    .withFromStop(FROM)
    .withToStop(TO)
    .withIsBidirectional(true)
    .withTraversalTime(TRAVERSAL_TIME)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withSignpostedAs(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withSignpostedAs("v2").build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(subject, copy);
    assertEquals(subject, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.getSignpostedAs());
    assertEquals(MODE, copy.getPathwayMode());
    assertEquals(FROM, copy.getFromStop());
    assertEquals(TO, copy.getToStop());
    assertNull(copy.getReverseSignpostedAs());
    assertEquals(TRAVERSAL_TIME, copy.getTraversalTime());
    assertEquals(0, copy.getLength());
    assertEquals(0, copy.getStairCount());
    assertEquals(0, copy.getSlope());
    assertEquals(0, copy.getLength());
    assertTrue(copy.isBidirectional());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withSignpostedAs("X").build()));
    assertFalse(subject.sameAs(subject.copy().withReverseSignpostedAs("X").build()));
    assertFalse(subject.sameAs(subject.copy().withPathwayMode(PathwayMode.ELEVATOR).build()));
    assertFalse(subject.sameAs(subject.copy().withTraversalTime(200).build()));
    assertFalse(subject.sameAs(subject.copy().withSlope(1).build()));
    assertFalse(subject.sameAs(subject.copy().withIsBidirectional(false).build()));
  }
}

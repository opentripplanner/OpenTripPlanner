package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class FareZoneTest {

  private static final String ID = "1";
  private static final String NAME = "name";
  private static final FareZone SUBJECT = FareZone.of(TimetableRepositoryForTest.id(ID))
    .withName(NAME)
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
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName("X").build()));
  }
}

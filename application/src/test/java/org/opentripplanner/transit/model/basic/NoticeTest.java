package org.opentripplanner.transit.model.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class NoticeTest {

  private static final String ID = "1";
  private static final String TEXT = "text";
  private static final String PUBLIC_CODE = "public code";

  private static final Notice subject = Notice.of(TimetableRepositoryForTest.id(ID))
    .withPublicCode(PUBLIC_CODE)
    .withText(TEXT)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy, and set the same publicCode (nothing is changed)
    var copy = subject.copy().withPublicCode(PUBLIC_CODE).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withPublicCode("v2").build();

    // The two objects are not the same instance, but is equal(same id)
    assertNotSame(subject, copy);
    assertEquals(subject, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.publicCode());
    assertEquals(TEXT, copy.text());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withPublicCode("X").build()));
    assertFalse(subject.sameAs(subject.copy().withText("X").build()));
  }
}

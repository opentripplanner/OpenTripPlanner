package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

public class BrandingTest {

  private static final String ID = "Branding:1";
  private static final String SHORT_NAME = "test_short_name";
  private static final String NAME = "test_name";
  private static final String URL = "test_url";
  private static final String DESCRIPTION = "test_description";
  private static final String IMAGE = "test_image";

  Branding subject = Branding.of(TimetableRepositoryForTest.id(ID))
    .withShortName(SHORT_NAME)
    .withName(NAME)
    .withUrl(URL)
    .withDescription(DESCRIPTION)
    .withImage(IMAGE)
    .build();

  @Test
  void copy() {
    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withName(NAME).build();

    // Same object should be returned if nothing changed
    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withName("v2").build();

    // The two objects are not the same instance, but is equal(same id)
    assertNotSame(copy, subject);
    assertEquals(copy, subject);

    assertEquals(ID, copy.getId().getId());
    assertEquals(SHORT_NAME, copy.getShortName());
    assertEquals("v2", copy.getName());
    assertEquals(URL, copy.getUrl());
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(IMAGE, copy.getImage());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withName("X").build()));
    assertFalse(subject.sameAs(subject.copy().withShortName("X").build()));
    assertFalse(subject.sameAs(subject.copy().withUrl("X").build()));
    assertFalse(subject.sameAs(subject.copy().withDescription("X").build()));
    assertFalse(subject.sameAs(subject.copy().withImage("X").build()));
  }
}

package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

public class GroupOfRoutesTest {

  private static final String ID = "Test:GroupOfLines:1";
  private static final String PRIVATE_CODE = "test_private_code";
  private static final String SHORT_NAME = "test_short_name";
  private static final String NAME = "test_name";
  private static final String DESCRIPTION = "description";

  private static final GroupOfRoutes SUBJECT = GroupOfRoutes.of(TimetableRepositoryForTest.id(ID))
    .withPrivateCode(PRIVATE_CODE)
    .withShortName(SHORT_NAME)
    .withName(NAME)
    .withDescription(DESCRIPTION)
    .build();

  @Test
  public void copy() {
    // Make a copy, and set the same name (nothing is changed)
    var copy = SUBJECT.copy().withName(NAME).build();

    // Same object should be returned if nothing changed
    assertSame(SUBJECT, copy);

    // Copy and change name
    copy = SUBJECT.copy().withName("v2").build();

    // The two objects are not the same instance, but is equal(same id)
    assertNotSame(copy, SUBJECT);
    assertEquals(copy, SUBJECT);

    assertEquals(ID, copy.getId().getId());
    assertEquals(PRIVATE_CODE, copy.getPrivateCode());
    assertEquals(SHORT_NAME, copy.getShortName());
    assertEquals("v2", copy.getName());
    assertEquals(DESCRIPTION, copy.getDescription());
  }

  @Test
  public void sameValue() {
    // Make a copy, and set the same name (nothing is changed)
    var other = SUBJECT.copy().build();
    assertTrue(SUBJECT.sameAs(other));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withDescription("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withShortName("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withPrivateCode("X").build()));
  }
}

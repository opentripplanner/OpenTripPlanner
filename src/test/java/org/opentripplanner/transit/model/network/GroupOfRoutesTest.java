package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class GroupOfRoutesTest {

  private static final String ID = "Test:GroupOfLines:1";
  private static final String PRIVATE_CODE = "test_private_code";
  private static final String SHORT_NAME = "test_short_name";
  private static final String NAME = "test_name";
  private static final String DESCRIPTION = "description";

  private static final GroupOfRoutes subject = GroupOfRoutes
    .of(TransitModelForTest.id(ID))
    .withPrivateCode(PRIVATE_CODE)
    .withShortName(SHORT_NAME)
    .withName(NAME)
    .withDescription(DESCRIPTION)
    .build();

  @Test
  @SuppressWarnings("ConstantConditions")
  public void copy() {
    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withName(NAME).build();

    // Same object should be returned if nothing changed
    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withName("v2").build();

    // The two objects are not he same instance, but is equal(sae id)
    assertNotSame(copy, subject);
    assertEquals(copy, subject);

    assertEquals(ID, copy.getId().getId());
    assertEquals(PRIVATE_CODE, copy.getPrivateCode());
    assertEquals(SHORT_NAME, copy.getShortName());
    assertEquals("v2", copy.getName());
    assertEquals(DESCRIPTION, copy.getDescription());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testToString() {
    assertEquals(
      "GroupOfRoutes{" +
      "id: F:Test:GroupOfLines:1, " +
      "privateCode: 'test_private_code', " +
      "shortName: 'test_short_name', " +
      "name: 'test_name', " +
      "description: 'description'" +
      "}",
      subject.toString()
    );
  }
}

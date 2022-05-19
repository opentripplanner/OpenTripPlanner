package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;

class OperatorTest {

  private static final String ID = "1";
  private static final String NAME = "name";
  private static final String URL = "http://info.aaa.com";
  private static final String PHONE = "+47 95566333";

  private static final Operator subject = Operator
    .of(TransitModelForTest.id(ID))
    .setName(NAME)
    .setUrl(URL)
    .setPhone(PHONE)
    .build();

  @Test
  void copy() {
    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().setName(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().setName("v2").build();

    // The two objects are not he same instance, but is equal(sae id)
    assertNotSame(copy, subject);
    assertEquals(copy, subject);

    assertEquals(copy.getId().getId(), ID);
    assertEquals("v2", copy.getName());
    assertEquals(copy.getUrl(), URL);
    assertEquals(copy.getPhone(), PHONE);
  }

  @Test
  void testToString() {
    assertEquals(subject.toString(), "<Operator F:1>");
  }
}

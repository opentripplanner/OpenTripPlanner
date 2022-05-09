package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    assertEquals(subject.getId().getId(), ID);
    var copy = subject.copy().setId(TransitModelForTest.id("v2")).build();

    assertEquals(copy.getId().getId(), "v2");
    assertEquals(copy.getName(), NAME);
    assertEquals(copy.getUrl(), URL);
    assertEquals(copy.getPhone(), PHONE);
  }

  @Test
  void testToString() {
    assertEquals(subject.toString(), "<Operator F:1>");
  }
}

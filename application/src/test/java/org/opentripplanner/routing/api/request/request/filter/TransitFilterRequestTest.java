package org.opentripplanner.routing.api.request.request.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransitFilterRequestTest {

  private static final SelectRequest SELECT = SelectRequest.of()
    .withAgenciesFromString("A:1")
    .build();

  @Test
  void testToString() {
    assertEquals("ALL", TransitFilterRequest.of().build().toString());
    assertEquals(
      "(select: [(transportModes: EMPTY, agencies: [A:1])])",
      TransitFilterRequest.of().addSelect(SELECT).build().toString()
    );
  }
}

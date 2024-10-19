package org.opentripplanner.transit.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EntityNotFoundExceptionTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");

  @Test
  void getMessage() {
    assertEquals(
      "Integer entity not found: F:1",
      new EntityNotFoundException(Integer.class, ID).getMessage()
    );
    assertEquals(
      "Stop or Station entity not found: F:1",
      new EntityNotFoundException("Stop or Station", ID).getMessage()
    );
  }
}

package org.opentripplanner.service.worldenvelope.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

class WorldEnvelopeRepositoryTest {

  private final WorldEnvelope envelope = WorldEnvelope.of()
    .expandToIncludeStreetEntities(60d, 10d)
    .expandToIncludeStreetEntities(65d, 14d)
    .build();

  @Test
  void normalModelFlow() {
    var subject = new DefaultWorldEnvelopeRepository();

    assertTrue(subject.retrieveEnvelope().isEmpty());

    subject.saveEnvelope(envelope);
    assertEquals(envelope, subject.retrieveEnvelope().get());
  }
}

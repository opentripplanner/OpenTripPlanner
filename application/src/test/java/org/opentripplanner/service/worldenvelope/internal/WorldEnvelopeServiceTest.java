package org.opentripplanner.service.worldenvelope.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

class WorldEnvelopeServiceTest {

  private final WorldEnvelope envelope = WorldEnvelope.of()
    .expandToIncludeStreetEntities(60d, 10d)
    .expandToIncludeStreetEntities(65d, 14d)
    .build();

  @Test
  void normalModelFlow() {
    var repository = new DefaultWorldEnvelopeRepository();
    var subject = new DefaultWorldEnvelopeService(repository);

    assertTrue(subject.envelope().isEmpty());

    repository.saveEnvelope(envelope);

    assertEquals(envelope, subject.envelope().get());
  }
}

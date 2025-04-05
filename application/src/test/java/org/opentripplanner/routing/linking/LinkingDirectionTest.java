package org.opentripplanner.routing.linking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.edge.LinkingDirection;

class LinkingDirectionTest {

  @Test
  void allowOutgoing() {
    assertFalse(LinkingDirection.INCOMING.allowOutgoing());
    assertTrue(LinkingDirection.OUTGOING.allowOutgoing());
    assertTrue(LinkingDirection.BIDIRECTIONAL.allowOutgoing());
  }

  @Test
  void allowIncoming() {
    assertTrue(LinkingDirection.INCOMING.allowIncoming());
    assertFalse(LinkingDirection.OUTGOING.allowIncoming());
    assertTrue(LinkingDirection.BIDIRECTIONAL.allowIncoming());
  }
}

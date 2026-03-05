package org.opentripplanner.routing.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.core.model.id.FeedScopedId;

class PassThroughViaLocationTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");

  private static final String LABEL = "AName";

  @SuppressWarnings("DataFlowIssue")
  private static final ViaLocation SUBJECT = new PassThroughViaLocation(LABEL, List.of(ID));

  @Test
  void allowAsPassThroughPoint() {
    assertTrue(SUBJECT.isPassThroughLocation());
  }

  @Test
  void minimumWaitTime() {
    assertEquals(Duration.ZERO, SUBJECT.minimumWaitTime());
  }

  @Test
  void label() {
    assertEquals(LABEL, SUBJECT.label());
  }

  @Test
  void stopLocationIds() {
    assertEquals("[F:1]", SUBJECT.stopLocationIds().toString());
  }

  @Test
  void coordinates() {
    assertTrue(SUBJECT.coordinate().isEmpty());
  }

  @Test
  void testToString() {
    assertEquals(
      "PassThroughViaLocation{label: AName, stopLocationIds: [F:1]}",
      SUBJECT.toString()
    );
  }

  @Test
  void testEqAndHashCode() {
    AssertEqualsAndHashCode.verify(SUBJECT)
      .sameAs(new PassThroughViaLocation(SUBJECT.label(), SUBJECT.stopLocationIds()))
      .differentFrom(
        new PassThroughViaLocation("Other", SUBJECT.stopLocationIds()),
        new PassThroughViaLocation(SUBJECT.label(), List.of(new FeedScopedId("F", "2")))
      );
  }
}

package org.opentripplanner.routing.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class PassThroughViaLocationTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");

  private static final String LABEL = "AName";

  @SuppressWarnings("DataFlowIssue")
  private static final ViaLocation subject = new PassThroughViaLocation(LABEL, List.of(ID));

  @Test
  void allowAsPassThroughPoint() {
    assertTrue(subject.isPassThroughLocation());
  }

  @Test
  void minimumWaitTime() {
    assertEquals(Duration.ZERO, subject.minimumWaitTime());
  }

  @Test
  void label() {
    assertEquals(LABEL, subject.label());
  }

  @Test
  void stopLocationIds() {
    assertEquals("[F:1]", subject.stopLocationIds().toString());
  }

  @Test
  void coordinates() {
    assertEquals("[]", subject.coordinates().toString());
  }

  @Test
  void testToString() {
    assertEquals(
      "PassThroughViaLocation{label: AName, stopLocationIds: [F:1]}",
      subject.toString()
    );
  }

  @Test
  void testEqAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(new PassThroughViaLocation(subject.label(), subject.stopLocationIds()))
      .differentFrom(
        new PassThroughViaLocation("Other", subject.stopLocationIds()),
        new PassThroughViaLocation(subject.label(), List.of(new FeedScopedId("F", "2")))
      );
  }
}

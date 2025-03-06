package org.opentripplanner.routing.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class VisitViaLocationTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");
  private static final String LABEL = "AName";
  private static final Duration MINIMUM_WAIT_TIME = Duration.ofMinutes(5);

  @SuppressWarnings("DataFlowIssue")
  private static final ViaLocation subject = new VisitViaLocation(
    LABEL,
    MINIMUM_WAIT_TIME,
    List.of(ID),
    List.of(WgsCoordinate.GREENWICH)
  );

  @Test
  void allowAsPassThroughPoint() {
    assertFalse(subject.isPassThroughLocation());
  }

  @Test
  void minimumWaitTime() {
    assertEquals(MINIMUM_WAIT_TIME, subject.minimumWaitTime());
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
    assertEquals("[" + WgsCoordinate.GREENWICH + "]", subject.coordinates().toString());
  }

  @Test
  void testToString() {
    assertEquals(
      "VisitViaLocation{label: AName, minimumWaitTime: 5m, stopLocationIds: [F:1], coordinates: [(51.48, 0.0)]}",
      subject.toString()
    );
  }

  @Test
  void testEqAndHashCode() {
    var l = subject.label();
    var mwt = subject.minimumWaitTime();
    var ids = subject.stopLocationIds();
    var cs = subject.coordinates();

    AssertEqualsAndHashCode.verify(subject)
      .sameAs(new VisitViaLocation(l, mwt, ids, cs))
      .differentFrom(
        new VisitViaLocation("other", mwt, ids, cs),
        new VisitViaLocation(l, Duration.ZERO, ids, cs),
        new VisitViaLocation(l, mwt, List.of(), cs),
        new VisitViaLocation(l, mwt, ids, List.of())
      );
  }
}

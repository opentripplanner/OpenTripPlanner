package org.opentripplanner.routing.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;

class VisitViaLocationTest {

  private static final FeedScopedId ID = FeedScopedId.ofNullable("F", "1");
  private static final String LABEL = "AName";
  private static final Duration MINIMUM_WAIT_TIME = Duration.ofMinutes(5);

  @SuppressWarnings("DataFlowIssue")
  private static final ViaLocation SUBJECT = new VisitViaLocation(
    LABEL,
    MINIMUM_WAIT_TIME,
    List.of(ID),
    WgsCoordinate.GREENWICH
  );

  @Test
  void allowAsPassThroughPoint() {
    assertFalse(SUBJECT.isPassThroughLocation());
  }

  @Test
  void minimumWaitTime() {
    assertEquals(MINIMUM_WAIT_TIME, SUBJECT.minimumWaitTime());
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
    assertEquals(WgsCoordinate.GREENWICH, SUBJECT.coordinate().get());
  }

  @Test
  void testToString() {
    assertEquals(
      "VisitViaLocation{label: AName, minimumWaitTime: 5m, stopLocationIds: [F:1], coordinate: (51.48, 0.0)}",
      SUBJECT.toString()
    );
  }

  @Test
  void testEqAndHashCode() {
    var l = SUBJECT.label();
    var mwt = SUBJECT.minimumWaitTime();
    var ids = SUBJECT.stopLocationIds();
    var cs = SUBJECT.coordinate();

    AssertEqualsAndHashCode.verify(SUBJECT)
      .sameAs(new VisitViaLocation(l, mwt, ids, cs.orElse(null)))
      .differentFrom(
        new VisitViaLocation("other", mwt, ids, cs.orElse(null)),
        new VisitViaLocation(l, Duration.ZERO, ids, cs.orElse(null)),
        new VisitViaLocation(l, mwt, List.of(), cs.orElse(null)),
        new VisitViaLocation(l, mwt, ids, null)
      );
  }
}

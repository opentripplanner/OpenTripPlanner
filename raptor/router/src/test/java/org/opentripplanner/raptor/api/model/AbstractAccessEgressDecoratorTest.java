package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;

class AbstractAccessEgressDecoratorTest {

  private static final int DURATION = 600;
  private static final int SLACK = 60;
  private static final int STOP = 7;
  private static final TestAccessEgress DELEGATE = TestAccessEgress.walk(STOP, DURATION);
  private static final TestAccessEgress DELEGATE_W_OPENING_HOURS = DELEGATE.openingHours(
    "10:00",
    "10:35"
  );

  private final RaptorAccessEgress subject =
    AbstractAccessEgressDecorator.accessEgressWithExtraSlack(DELEGATE, SLACK);
  private final RaptorAccessEgress subjectWOpeningHours =
    AbstractAccessEgressDecorator.accessEgressWithExtraSlack(DELEGATE_W_OPENING_HOURS, SLACK);

  private AbstractAccessEgressDecorator subjectCast() {
    return (AbstractAccessEgressDecorator) subject;
  }

  @Test
  void resolveDelegate() {
    assertEquals(
      DELEGATE,
      AbstractAccessEgressDecorator.findType(subject, TestAccessEgress.class).orElseThrow()
    );
    assertEquals(
      DELEGATE,
      AbstractAccessEgressDecorator.findType(DELEGATE, TestAccessEgress.class).orElseThrow()
    );
  }

  @Test
  void createAccessEgressWithExtraSlackFromDuration() {
    var fromDuration = AbstractAccessEgressDecorator.accessEgressWithExtraSlack(
      DELEGATE,
      Duration.ofSeconds(SLACK)
    );
    assertEquals(subject.durationInSeconds(), fromDuration.durationInSeconds());
  }

  @Test
  void delegate() {
    assertEquals(DELEGATE, subjectCast().delegate());
  }

  @Test
  void stop() {
    assertEquals(STOP, subject.stop());
    assertEquals(DELEGATE.stop(), subject.stop());
  }

  @Test
  void c1() {
    assertEquals(DELEGATE.c1(), subject.c1());
  }

  @Test
  void durationInSeconds() {
    assertEquals(DELEGATE.durationInSeconds() + SLACK, subject.durationInSeconds());
  }

  @Test
  void timePenalty() {}

  @Test
  void hasTimePenalty() {
    assertFalse(subject.hasTimePenalty());
  }

  @Test
  void earliestDepartureTime() {
    assertEquals(100, subject.earliestDepartureTime(100));
  }

  @Test
  void latestArrivalTime() {
    assertEquals(100, subject.latestArrivalTime(100));
  }

  @Test
  void hasOpeningHours() {
    assertFalse(subject.hasOpeningHours());
    assertTrue(subjectWOpeningHours.hasOpeningHours());
  }

  @Test
  void openingHoursToString() {
    assertNull(subject.openingHoursToString());
    assertEquals("Open(10:00 10:35)", subjectWOpeningHours.openingHoursToString());
  }

  @Test
  void numberOfRides() {
    assertEquals(0, subject.numberOfRides());
  }

  @Test
  void hasRides() {
    assertFalse(subject.hasRides());
  }

  @Test
  void testToString() {
    assertEquals("Walk 10m C₁1_200 ~ 7", subject.toString());
    assertEquals("Walk 10m C₁1_200 Open(10:00 10:35) ~ 7", subjectWOpeningHours.toString());
  }
}

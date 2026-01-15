package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onebusaway.gtfs.model.BookingRule.NO_VALUE;

import java.time.Duration;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.BookingRule;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

class BookingRuleMapperTest {

  private final BookingRuleMapper subject = new BookingRuleMapper();

  @Test
  void mapNullIsNull() {
    assertNull(subject.map(null));
  }

  @Test
  void mapContactInfoAndMessages() {
    var rule = rule("1");
    rule.setPhoneNumber("123");
    rule.setInfoUrl("https://info");
    rule.setUrl("https://book");
    rule.setMessage("msg");
    rule.setPickupMessage("pmsg");
    rule.setDropOffMessage("dmsg");

    var mapped = subject.map(rule);
    assertNotNull(mapped);

    assertEquals("123", mapped.getContactInfo().getPhoneNumber());
    assertEquals("https://info", mapped.getContactInfo().getInfoUrl());
    assertEquals("https://book", mapped.getContactInfo().getBookingUrl());
    assertEquals("msg", mapped.getMessage());
    assertEquals("pmsg", mapped.getPickupMessage());
    assertEquals("dmsg", mapped.getDropOffMessage());

    assertNull(mapped.getLatestBookingTime());
    assertNull(mapped.getLatestBookingTime());
    assertThat(mapped.getMinimumBookingNotice()).isEmpty();
    assertThat(mapped.getMaximumBookingNotice()).isEmpty();
  }

  @Test
  void mapEarliestAndLatestBookingTime() {
    var rule = rule("2");
    // earliest: 10:00, 1 day prior
    rule.setPriorNoticeStartTime(LocalTime.of(10, 0).toSecondOfDay());
    rule.setPriorNoticeStartDay(1);
    // latest: 12:00, 0 days prior
    rule.setPriorNoticeLastTime(LocalTime.of(12, 0).toSecondOfDay());
    rule.setPriorNoticeLastDay(0);

    var mapped = subject.map(rule);
    assertNotNull(mapped);

    BookingTime earliest = mapped.getEarliestBookingTime();
    BookingTime latest = mapped.getLatestBookingTime();

    assertNotNull(earliest);
    assertEquals(LocalTime.of(10, 0), earliest.getTime());
    assertEquals(1, earliest.getDaysPrior());

    assertNotNull(latest);
    assertEquals(LocalTime.of(12, 0), latest.getTime());
    assertEquals(0, latest.getDaysPrior());

    // When earliest/latest are set, min/max notice should be ignored
    assertTrue(mapped.getMinimumBookingNotice().isEmpty());
    assertTrue(mapped.getMaximumBookingNotice().isEmpty());
  }

  @Test
  void mapNoEarliestOrLatestFallsBackToMinMaxNotice() {
    var rule = rule("3");
    // when either of prior notice time and day are set to -999(NO_VALUE), they should be treated as "not set".
    rule.setPriorNoticeStartTime(NO_VALUE);
    rule.setPriorNoticeStartDay(NO_VALUE);
    rule.setPriorNoticeLastTime(NO_VALUE);
    rule.setPriorNoticeLastDay(NO_VALUE);

    rule.setPriorNoticeDurationMin(45);
    rule.setPriorNoticeDurationMax(120);

    var mapped = subject.map(rule);
    assertNull(mapped.getEarliestBookingTime());
    assertNull(mapped.getLatestBookingTime());
    assertEquals(Duration.ofMinutes(45), mapped.getMinimumBookingNotice().orElseThrow());
    assertEquals(Duration.ofMinutes(120), mapped.getMaximumBookingNotice().orElseThrow());
  }

  @Test
  void mapCacheReturnsSameInstanceForSameRule() {
    var rule = rule("4");
    var r1 = subject.map(rule);
    var r2 = subject.map(rule);
    assertSame(r1, r2);
  }

  @Test
  void acceptZeroAsMidnight() {
    var rule = rule("5");
    rule.setPriorNoticeStartTime(0);
    rule.setPriorNoticeStartDay(10);
    rule.setPriorNoticeLastTime(0);
    rule.setPriorNoticeLastDay(5);

    var mapped = subject.map(rule);
    assertEquals("00:00-10d", mapped.getEarliestBookingTime().toString());
    assertEquals("00:00-5d", mapped.getLatestBookingTime().toString());
  }

  private static BookingRule rule(String id) {
    var r = new BookingRule();
    r.setId(new AgencyAndId("A", id));
    return r;
  }
}

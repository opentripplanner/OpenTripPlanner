package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class AgencyTest {

  private static final String ID = "1";
  private static final String NAME = "name";
  private static final String URL = "http://info.aaa.com";
  private static final String TIMEZONE = "Europe/Oslo";
  private static final String PHONE = "+47 95566333";
  private static final String FARE_URL = "http://fare.aaa.com";
  private static final String LANG = "image";

  private static final Agency SUBJECT = Agency.of(TimetableRepositoryForTest.id(ID))
    .withName(NAME)
    .withUrl(URL)
    .withTimezone(TIMEZONE)
    .withPhone(PHONE)
    .withFareUrl(FARE_URL)
    .withLang(LANG)
    .build();

  @Test
  void copy() {
    assertEquals(ID, SUBJECT.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = SUBJECT.copy().withName(NAME).build();

    assertSame(SUBJECT, copy);

    // Copy and change name
    copy = SUBJECT.copy().withName("v2").build();

    // The two objects are not the same instance, but is equal(same id)
    assertNotSame(SUBJECT, copy);
    assertEquals(SUBJECT, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.getName());
    assertEquals(URL, copy.getUrl());
    assertEquals(TIMEZONE, copy.getTimezone().getId());
    assertEquals(PHONE, copy.getPhone());
    assertEquals(FARE_URL, copy.getFareUrl());
    assertEquals(LANG, copy.getLang());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withName("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withUrl("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withTimezone("CET").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withPhone("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withFareUrl("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withLang("X").build()));
  }
}

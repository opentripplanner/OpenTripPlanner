package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;

class AgencyTest {

  private static final String ID = "1";
  private static final String BRANDING_URL = "http://branding.aaa.com";
  private static final String NAME = "name";
  private static final String URL = "http://info.aaa.com";
  private static final String TIMEZONE = "Europe/Oslo";
  private static final String PHONE = "+47 95566333";
  private static final String FARE_URL = "http://fare.aaa.com";
  private static final String LANG = "image";

  private static final Agency subject = Agency
    .of(TransitModelForTest.id(ID))
    .withName(NAME)
    .withUrl(URL)
    .withTimezone(TIMEZONE)
    .withPhone(PHONE)
    .withBrandingUrl(BRANDING_URL)
    .withFareUrl(FARE_URL)
    .withLang(LANG)
    .build();

  @Test
  void copy() {
    assertEquals(subject.getId().getId(), ID);

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withName(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withName("v2").build();

    // The two objects are not he same instance, but is equal(sae id)
    assertNotSame(copy, subject);
    assertEquals(copy, subject);

    assertEquals(copy.getId().getId(), ID);
    assertEquals("v2", copy.getName());
    assertEquals(URL, copy.getUrl());
    assertEquals(TIMEZONE, copy.getTimezone());
    assertEquals(PHONE, copy.getPhone());
    assertEquals(BRANDING_URL, copy.getBrandingUrl());
    assertEquals(FARE_URL, copy.getFareUrl());
    assertEquals(LANG, copy.getLang());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameValue(subject.copy().build()));
    assertFalse(subject.sameValue(subject.copy().withId(TransitModelForTest.id("X")).build()));
    assertFalse(subject.sameValue(subject.copy().withName("X").build()));
    assertFalse(subject.sameValue(subject.copy().withUrl("X").build()));
    assertFalse(subject.sameValue(subject.copy().withTimezone("X").build()));
    assertFalse(subject.sameValue(subject.copy().withPhone("X").build()));
    assertFalse(subject.sameValue(subject.copy().withBrandingUrl("X").build()));
    assertFalse(subject.sameValue(subject.copy().withFareUrl("X").build()));
    assertFalse(subject.sameValue(subject.copy().withLang("X").build()));
  }
}

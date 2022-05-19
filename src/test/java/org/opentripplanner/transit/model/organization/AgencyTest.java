package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    .setName(NAME)
    .setUrl(URL)
    .setTimezone(TIMEZONE)
    .setPhone(PHONE)
    .setBrandingUrl(BRANDING_URL)
    .setFareUrl(FARE_URL)
    .setLang(LANG)
    .build();

  @Test
  void copy() {
    assertEquals(subject.getId().getId(), ID);

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().setName(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().setName("v2").build();

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
  void testToString() {
    assertEquals(subject.toString(), "<Agency F:1>");
  }
}

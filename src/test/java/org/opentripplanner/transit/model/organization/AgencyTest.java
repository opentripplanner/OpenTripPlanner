package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    var copy = subject.copy().setId(TransitModelForTest.id("v2")).build();

    assertEquals(copy.getId().getId(), "v2");
    assertEquals(copy.getName(), NAME);
    assertEquals(copy.getUrl(), URL);
    assertEquals(copy.getTimezone(), TIMEZONE);
    assertEquals(copy.getPhone(), PHONE);
    assertEquals(copy.getBrandingUrl(), BRANDING_URL);
    assertEquals(copy.getFareUrl(), FARE_URL);
    assertEquals(copy.getLang(), LANG);
  }

  @Test
  void testToString() {
    assertEquals(subject.toString(), "<Agency F:1>");
  }
}

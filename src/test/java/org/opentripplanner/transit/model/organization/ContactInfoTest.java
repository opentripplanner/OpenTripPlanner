package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ContactInfoTest {

  private static final String CONTACT_PERSON = "name";
  private static final String PHONE = "+47 95566333";
  private static final String FAX_NUMBER = "+47 99966001";
  private static final String ADD_DETAILS = "Extra info";
  private static final String BOOKING_URL = "http://book.aaa.com";
  private static final String EMAIL = "email@aaa.com";
  private static final String INFO_URL = "http://info.aaa.com";

  private static final ContactInfo subject = ContactInfo
    .of()
    .setContactPerson(CONTACT_PERSON)
    .setPhoneNumber(PHONE)
    .seteMail(EMAIL)
    .setFaxNumber(FAX_NUMBER)
    .setInfoUrl(INFO_URL)
    .setBookingUrl(BOOKING_URL)
    .setAdditionalDetails(ADD_DETAILS)
    .build();

  @Test
  void copy() {
    var copy = subject.copy().build();

    assertEquals(copy.getContactPerson(), CONTACT_PERSON);
    assertEquals(copy.getPhoneNumber(), PHONE);
    assertEquals(copy.geteMail(), EMAIL);
    assertEquals(copy.getFaxNumber(), FAX_NUMBER);
    assertEquals(copy.getInfoUrl(), INFO_URL);
    assertEquals(copy.getBookingUrl(), BOOKING_URL);
    assertEquals(copy.getAdditionalDetails(), ADD_DETAILS);
  }

  @Test
  void testToString() {
    assertEquals(
      "ContactInfo{" +
      "contactPerson: 'name', " +
      "phoneNumber: '+47 95566333', " +
      "eMail: 'email@aaa.com', " +
      "faxNumber: '+47 99966001', " +
      "infoUrl: 'http://info.aaa.com', " +
      "bookingUrl: 'http://book.aaa.com', " +
      "additionalDetails: 'Extra info'" +
      "}",
      subject.toString()
    );
  }
}

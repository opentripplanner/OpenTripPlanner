package org.opentripplanner.transit.model.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ContactInfoTest {

  private static final String CONTACT_PERSON = "name";
  private static final String PHONE = "+47 95566333";
  private static final String FAX_NUMBER = "+47 99966001";
  private static final String ADD_DETAILS = "Extra info";
  private static final String BOOKING_URL = "http://book.aaa.com";
  private static final String EMAIL = "email@aaa.com";
  private static final String INFO_URL = "http://info.aaa.com";

  private static final ContactInfo subject = ContactInfo.of()
    .withContactPerson(CONTACT_PERSON)
    .withPhoneNumber(PHONE)
    .withEMail(EMAIL)
    .withFaxNumber(FAX_NUMBER)
    .withInfoUrl(INFO_URL)
    .withBookingUrl(BOOKING_URL)
    .withAdditionalDetails(ADD_DETAILS)
    .build();

  @Test
  void copy() {
    // Create a copy, but do not change it
    var copy = subject.copy().withContactPerson(CONTACT_PERSON).build();

    // Then the build object should be the same
    assertSame(subject, copy);

    // Copy and change the contact person
    copy = subject.copy().withContactPerson("New Person").build();

    assertNotSame(subject, copy);
    assertEquals("New Person", copy.getContactPerson());
    assertEquals(PHONE, copy.getPhoneNumber());
    assertEquals(EMAIL, copy.geteMail());
    assertEquals(FAX_NUMBER, copy.getFaxNumber());
    assertEquals(INFO_URL, copy.getInfoUrl());
    assertEquals(BOOKING_URL, copy.getBookingUrl());
    assertEquals(ADD_DETAILS, copy.getAdditionalDetails());
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

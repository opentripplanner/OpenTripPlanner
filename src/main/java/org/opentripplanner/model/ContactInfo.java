package org.opentripplanner.model;

import java.io.Serializable;

public class ContactInfo implements Serializable {

  private final String contactPerson;

  private final String phoneNumber;

  private final String eMail;

  private final String faxNumber;

  private final String infoUrl;

  private final String bookingUrl;

  private final String additionalDetails;

  public ContactInfo(
      String contactPerson,
      String phoneNumber,
      String eMail,
      String faxNumber,
      String infoUrl,
      String bookingUrl,
      String additionalDetails
  ) {
    this.contactPerson = contactPerson;
    this.phoneNumber = phoneNumber;
    this.eMail = eMail;
    this.faxNumber = faxNumber;
    this.infoUrl = infoUrl;
    this.bookingUrl = bookingUrl;
    this.additionalDetails = additionalDetails;
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String geteMail() {
    return eMail;
  }

  public String getFaxNumber() {
    return faxNumber;
  }

  public String getInfoUrl() {
    return infoUrl;
  }

  public String getBookingUrl() {
    return bookingUrl;
  }

  public String getAdditionalDetails() {
    return additionalDetails;
  }
}

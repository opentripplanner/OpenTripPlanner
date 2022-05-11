package org.opentripplanner.transit.model.organization;

import java.io.Serializable;
import org.opentripplanner.util.lang.ToStringBuilder;

public class ContactInfo implements Serializable {

  private final String contactPerson;

  private final String phoneNumber;

  private final String eMail;

  private final String faxNumber;

  private final String infoUrl;

  private final String bookingUrl;

  private final String additionalDetails;

  public ContactInfo(ContactInfoBuilder builder) {
    this.contactPerson = builder.getContactPerson();
    this.phoneNumber = builder.getPhoneNumber();
    this.eMail = builder.geteMail();
    this.faxNumber = builder.getFaxNumber();
    this.infoUrl = builder.getInfoUrl();
    this.bookingUrl = builder.getBookingUrl();
    this.additionalDetails = builder.getAdditionalDetails();
  }

  public static ContactInfoBuilder of() {
    return new ContactInfoBuilder();
  }

  public ContactInfoBuilder copy() {
    return new ContactInfoBuilder(this);
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

  @Override
  public String toString() {
    return ToStringBuilder
      .of(ContactInfo.class)
      .addStr("contactPerson", contactPerson)
      .addStr("phoneNumber", phoneNumber)
      .addStr("eMail", eMail)
      .addStr("faxNumber", faxNumber)
      .addStr("infoUrl", infoUrl)
      .addStr("bookingUrl", bookingUrl)
      .addStr("additionalDetails", additionalDetails)
      .toString();
  }
}

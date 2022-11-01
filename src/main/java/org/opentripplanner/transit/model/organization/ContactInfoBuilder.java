package org.opentripplanner.transit.model.organization;

import org.opentripplanner.transit.model.framework.AbstractBuilder;

public class ContactInfoBuilder extends AbstractBuilder<ContactInfo, ContactInfoBuilder> {

  private String contactPerson;
  private String phoneNumber;
  private String eMail;
  private String faxNumber;
  private String infoUrl;
  private String bookingUrl;
  private String additionalDetails;

  ContactInfoBuilder() {
    super(null);
  }

  ContactInfoBuilder(ContactInfo original) {
    super(original);
    this.contactPerson = original.getContactPerson();
    this.phoneNumber = original.getPhoneNumber();
    this.eMail = original.geteMail();
    this.faxNumber = original.getFaxNumber();
    this.infoUrl = original.getInfoUrl();
    this.bookingUrl = original.getBookingUrl();
    this.additionalDetails = original.getAdditionalDetails();
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public ContactInfoBuilder withContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
    return this;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public ContactInfoBuilder withPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public String geteMail() {
    return eMail;
  }

  public ContactInfoBuilder withEMail(String eMail) {
    this.eMail = eMail;
    return this;
  }

  public String getFaxNumber() {
    return faxNumber;
  }

  public ContactInfoBuilder withFaxNumber(String faxNumber) {
    this.faxNumber = faxNumber;
    return this;
  }

  public String getInfoUrl() {
    return infoUrl;
  }

  public ContactInfoBuilder withInfoUrl(String infoUrl) {
    this.infoUrl = infoUrl;
    return this;
  }

  public String getBookingUrl() {
    return bookingUrl;
  }

  public ContactInfoBuilder withBookingUrl(String bookingUrl) {
    this.bookingUrl = bookingUrl;
    return this;
  }

  public String getAdditionalDetails() {
    return additionalDetails;
  }

  public ContactInfoBuilder withAdditionalDetails(String additionalDetails) {
    this.additionalDetails = additionalDetails;
    return this;
  }

  @Override
  protected ContactInfo buildFromValues() {
    return new ContactInfo(this);
  }
}

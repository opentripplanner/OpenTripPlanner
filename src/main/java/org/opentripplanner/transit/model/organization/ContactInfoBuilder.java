package org.opentripplanner.transit.model.organization;

public class ContactInfoBuilder {

  private String contactPerson;

  private String phoneNumber;

  private String eMail;

  private String faxNumber;

  private String infoUrl;

  private String bookingUrl;

  private String additionalDetails;

  public ContactInfoBuilder() {}

  public ContactInfoBuilder(ContactInfo domain) {
    this.contactPerson = domain.getContactPerson();
    this.phoneNumber = domain.getPhoneNumber();
    this.eMail = domain.geteMail();
    this.faxNumber = domain.getFaxNumber();
    this.infoUrl = domain.getInfoUrl();
    this.bookingUrl = domain.getBookingUrl();
    this.additionalDetails = domain.getAdditionalDetails();
  }

  public String getContactPerson() {
    return contactPerson;
  }

  public ContactInfoBuilder setContactPerson(String contactPerson) {
    this.contactPerson = contactPerson;
    return this;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public ContactInfoBuilder setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public String geteMail() {
    return eMail;
  }

  public ContactInfoBuilder seteMail(String eMail) {
    this.eMail = eMail;
    return this;
  }

  public String getFaxNumber() {
    return faxNumber;
  }

  public ContactInfoBuilder setFaxNumber(String faxNumber) {
    this.faxNumber = faxNumber;
    return this;
  }

  public String getInfoUrl() {
    return infoUrl;
  }

  public ContactInfoBuilder setInfoUrl(String infoUrl) {
    this.infoUrl = infoUrl;
    return this;
  }

  public String getBookingUrl() {
    return bookingUrl;
  }

  public ContactInfoBuilder setBookingUrl(String bookingUrl) {
    this.bookingUrl = bookingUrl;
    return this;
  }

  public String getAdditionalDetails() {
    return additionalDetails;
  }

  public ContactInfoBuilder setAdditionalDetails(String additionalDetails) {
    this.additionalDetails = additionalDetails;
    return this;
  }
}

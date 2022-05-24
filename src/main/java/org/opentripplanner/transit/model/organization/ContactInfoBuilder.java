package org.opentripplanner.transit.model.organization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.AbstractBuilder;

public class ContactInfoBuilder extends AbstractBuilder<ContactInfo, ContactInfoBuilder> {

  private String contactPerson;
  private String phoneNumber;
  private String eMail;
  private String faxNumber;
  private String infoUrl;
  private String bookingUrl;
  private String additionalDetails;

  public ContactInfoBuilder() {
    super(null);
  }

  ContactInfoBuilder(ContactInfo original) {
    super(original);
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

  @Override
  protected void update(@Nonnull ContactInfo original) {
    this.contactPerson = original.getContactPerson();
    this.phoneNumber = original.getPhoneNumber();
    this.eMail = original.geteMail();
    this.faxNumber = original.getFaxNumber();
    this.infoUrl = original.getInfoUrl();
    this.bookingUrl = original.getBookingUrl();
    this.additionalDetails = original.getAdditionalDetails();
  }

  @Nullable
  @Override
  protected ContactInfo buildFromValues() {
    return new ContactInfo(this);
  }
}

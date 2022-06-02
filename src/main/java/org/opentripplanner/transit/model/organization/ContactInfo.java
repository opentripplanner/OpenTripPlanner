package org.opentripplanner.transit.model.organization;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.opentripplanner.transit.model.basic.TransitObject;
import org.opentripplanner.util.lang.ToStringBuilder;

public class ContactInfo implements TransitObject<ContactInfo, ContactInfoBuilder> {

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

  @NotNull
  public static ContactInfoBuilder of() {
    return new ContactInfoBuilder();
  }

  @NotNull
  public static ContactInfoBuilder ofNullable(@Nullable ContactInfo ci) {
    return new ContactInfoBuilder(ci);
  }

  @NotNull
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
  public boolean sameValue(@Nonnull ContactInfo other) {
    return equals(other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ContactInfo that = (ContactInfo) o;
    return (
      Objects.equals(contactPerson, that.contactPerson) &&
      Objects.equals(phoneNumber, that.phoneNumber) &&
      Objects.equals(eMail, that.eMail) &&
      Objects.equals(faxNumber, that.faxNumber) &&
      Objects.equals(infoUrl, that.infoUrl) &&
      Objects.equals(bookingUrl, that.bookingUrl) &&
      Objects.equals(additionalDetails, that.additionalDetails)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      contactPerson,
      phoneNumber,
      eMail,
      faxNumber,
      infoUrl,
      bookingUrl,
      additionalDetails
    );
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

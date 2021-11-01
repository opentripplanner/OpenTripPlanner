package org.opentripplanner.api.model;

import java.io.Serializable;

public class ApiContactInfo implements Serializable {

  public final String contactPerson;

  public final String phoneNumber;

  public final String eMail;

  public final String faxNumber;

  public final String infoUrl;

  public final String bookingUrl;

  public final String additionalDetails;

  public ApiContactInfo(
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
}

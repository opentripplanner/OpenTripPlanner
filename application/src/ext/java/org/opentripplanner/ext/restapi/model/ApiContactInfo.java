package org.opentripplanner.ext.restapi.model;

import java.io.Serializable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * How to contact the agency to book a trip or requests information.
 */
public class ApiContactInfo implements Serializable {

  /**
   * The person's name responsible to administer the trip.
   */
  public final String contactPerson;

  /**
   * Phone number to book the trip or request information.
   */
  public final String phoneNumber;

  /**
   * Email address to book the trip or request information.
   */
  public final String eMail;

  /**
   * Fax number to book the trip or request information. Very important.
   */
  public final String faxNumber;

  /**
   * URL to a website about general information about the service.
   */
  public final String infoUrl;

  /**
   * URL to a website to book the service.
   */
  public final String bookingUrl;

  /**
   * Any other comment that does not fit anywhere else.
   */
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

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
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

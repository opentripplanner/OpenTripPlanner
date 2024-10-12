package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiContactInfo;
import org.opentripplanner.transit.model.organization.ContactInfo;

public class ContactInfoMapper {

  static ApiContactInfo mapContactInfo(ContactInfo info) {
    if (info == null) {
      return null;
    }
    return new ApiContactInfo(
      info.getContactPerson(),
      info.getPhoneNumber(),
      info.geteMail(),
      info.getFaxNumber(),
      info.getInfoUrl(),
      info.getBookingUrl(),
      info.getAdditionalDetails()
    );
  }
}

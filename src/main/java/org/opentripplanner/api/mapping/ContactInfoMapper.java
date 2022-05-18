package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiContactInfo;
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

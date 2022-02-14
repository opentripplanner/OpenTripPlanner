package org.opentripplanner.api.rest.mapping;

import org.opentripplanner.api.rest.model.ApiContactInfo;
import org.opentripplanner.model.ContactInfo;

public class ContactInfoMapper {
    static ApiContactInfo mapContactInfo(ContactInfo info) {
        if (info == null) {return null;}
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

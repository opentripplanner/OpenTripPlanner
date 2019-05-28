package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Authority;

class AgencyMapper {
    Agency mapAgency(Authority authority, String timeZone){
        Agency agency = new Agency();

        agency.setId(authority.getId());
        agency.setName(authority.getName().getValue());
        agency.setTimezone(timeZone);

        if (authority.getContactDetails() != null) {
            agency.setUrl(authority.getContactDetails().getUrl());
            agency.setPhone(authority.getContactDetails().getPhone());
        }
        return agency;
    }

    Agency getDefaultAgency(String timeZone){
        Agency agency = new Agency();
        agency.setId("N/A");
        agency.setName("N/A");
        agency.setTimezone(timeZone);
        agency.setUrl("N/A");
        agency.setPhone("N/A");
        return agency;
    }
}

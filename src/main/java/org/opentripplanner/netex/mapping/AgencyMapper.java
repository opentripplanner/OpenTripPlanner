package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Operator;

public class AgencyMapper {
    public Agency mapAgency(Operator operator, String timeZone){
        Agency agency = new Agency();

        agency.setId(operator.getId());
        agency.setName(operator.getName().getValue());
        agency.setTimezone(timeZone);

        if (operator.getCustomerServiceContactDetails() != null) {
            agency.setUrl(operator.getCustomerServiceContactDetails().getUrl());
            agency.setPhone(operator.getCustomerServiceContactDetails().getPhone());
        }
        return agency;
    }
}

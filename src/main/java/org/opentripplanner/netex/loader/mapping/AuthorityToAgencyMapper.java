package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Agency;
import org.rutebanken.netex.model.Authority;

import static org.opentripplanner.netex.support.NetexObjectDecorator.withOptional;

/**
 * NeTEx authority is mapped to OTP agency. An authority is defined as "A company or organisation which is responsible
 * for the establishment of a public transport service." In NeTEx this is not the same as an operator. A default
 * authority can be created if none is present.
 */
class AuthorityToAgencyMapper {
    private final String timeZone;
    private final String dummyAgencyId;


    AuthorityToAgencyMapper(String timeZone) {
        this.timeZone = timeZone;
        this.dummyAgencyId = "Dummy-" + timeZone;
    }

    /**
     * Map authority and time zone to OTP agency.
     */
    Agency mapAuthorityToAgency(Authority source){
        Agency target = new Agency();

        target.setId(source.getId());
        target.setName(source.getName().getValue());
        target.setTimezone(timeZone);

        withOptional(source.getContactDetails(), c -> {
            target.setUrl(c.getUrl());
            target.setPhone(c.getPhone());
        });
        return target;
    }

    /**
     * Create a new dummy agency with time zone set. All other values are set to
     * "N/A" and id set to {@code "Dummy-" + timeZone}.
     */
    Agency createDummyAgency(){
        Agency agency = new Agency();
        agency.setId(dummyAgencyId);
        agency.setName("N/A");
        agency.setTimezone(timeZone);
        agency.setUrl("N/A");
        agency.setPhone("N/A");
        return agency;
    }

    String dummyAgencyId() {
       return dummyAgencyId;
    }
}

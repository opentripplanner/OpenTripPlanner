package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Agency;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.Authority;

import static org.opentripplanner.netex.mapping.support.NetexObjectDecorator.withOptional;

/**
 * NeTEx authority is mapped to OTP agency. An authority is defined as "A company or organisation which is responsible
 * for the establishment of a public transport service." In NeTEx this is not the same as an operator. A dummy
 * authority can be created if input data is missing an authority.
 */
class AuthorityToAgencyMapper {

    private final FeedScopedIdFactory idFactory;
    private final String timeZone;

    /**
     * This id is used to generate a "dummy" authority when the input data is not associated with an
     * authority. The OTP Model requires an agency to exist, while Netex do not.
     */
    private final String dummyAgencyId;


    AuthorityToAgencyMapper(FeedScopedIdFactory idFactory, String timeZone) {
        this.idFactory = idFactory;
        this.timeZone = timeZone;
        this.dummyAgencyId = "Dummy-" + timeZone;
    }

    /**
     * Map authority and time zone to OTP agency.
     */
    Agency mapAuthorityToAgency(Authority source){
        Agency target = new Agency(
            idFactory.createId(source.getId()),
            source.getName().getValue(),
            timeZone
        );

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
        Agency agency = new Agency(
            idFactory.createId(dummyAgencyId),
            "N/A",
            timeZone
        );
        agency.setUrl("N/A");
        agency.setPhone("N/A");
        return agency;
    }

    String dummyAgencyId() {
       return dummyAgencyId;
    }
}

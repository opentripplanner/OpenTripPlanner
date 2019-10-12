package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Operator;
import org.rutebanken.netex.model.ContactStructure;


/**
 * Maps a Netex operator to OTP Operator.
 */
class OperatorToAgencyMapper {
    /** private to prevent creating new instance of utility class with static methods only */
    private OperatorToAgencyMapper() {}

    static Operator mapOperator(org.rutebanken.netex.model.Operator source){
        Operator target = new Operator();

        target.setId(FeedScopedIdFactory.createFeedScopedId(source.getId()));
        target.setName(source.getName().getValue());

        mapContactDetails(source.getContactDetails(), target);

        return target;
    }

    private static void mapContactDetails(ContactStructure contactDetails, Operator target) {
        if(contactDetails == null) {
            return;
        }
        target.setUrl(contactDetails.getUrl());
        target.setPhone(contactDetails.getPhone());
    }
}

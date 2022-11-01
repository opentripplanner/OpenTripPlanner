package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.organization.OperatorBuilder;
import org.rutebanken.netex.model.ContactStructure;

/**
 * Maps a Netex operator to OTP Operator.
 */
class OperatorToAgencyMapper {

  private final FeedScopedIdFactory idFactory;

  OperatorToAgencyMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Operator mapOperator(org.rutebanken.netex.model.Operator source) {
    var target = Operator
      .of(idFactory.createId(source.getId()))
      .withName(source.getName().getValue());

    mapContactDetails(source.getContactDetails(), target);

    return target.build();
  }

  private static void mapContactDetails(ContactStructure contactDetails, OperatorBuilder target) {
    if (contactDetails == null) {
      return;
    }
    target.withUrl(contactDetails.getUrl());
    target.withPhone(contactDetails.getPhone());
  }
}

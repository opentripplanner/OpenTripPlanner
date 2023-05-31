package org.opentripplanner.netex.mapping;

import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.organization.OperatorBuilder;
import org.rutebanken.netex.model.ContactStructure;

/**
 * Maps a Netex operator to OTP Operator.
 */
class OperatorToAgencyMapper {

  private DataImportIssueStore issueStore;
  private final FeedScopedIdFactory idFactory;

  OperatorToAgencyMapper(DataImportIssueStore issueStore, FeedScopedIdFactory idFactory) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
  }

  Operator mapOperator(org.rutebanken.netex.model.Operator source) {
    final String name;
    if (source.getName() == null || StringUtils.hasNoValue(source.getName().getValue())) {
      issueStore.add("MissingOperatorName", "Missing name for operator %s", source.getId());
      // fall back to NeTEx id when the operator name is missing
      name = source.getId();
    } else {
      name = source.getName().getValue();
    }
    var target = Operator.of(idFactory.createId(source.getId())).withName(name);

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

package org.opentripplanner.apis.transmodel.model.plan;

import graphql.schema.GraphQLInputObjectType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;

public class BannedInputType {

  static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("InputBanned")
    .description(
      "Filter trips by disallowing lines involving certain elements. If both lines and " +
      "authorities are specified, only one must be valid for each line to be banned. If a " +
      "line is both banned and whitelisted, it will be counted as banned."
    )
    .field(GqlUtil.newIdListInputField("lines", "Set of ids for lines that should not be used"))
    .field(
      GqlUtil.newIdListInputField(
        "authorities",
        "Set of ids for authorities that should not be used"
      )
    )
    .field(
      GqlUtil.newIdListInputField(
        "quays",
        "NOT IMPLEMENTED. Set of ids of quays that should not be allowed for boarding or alighting. Trip patterns " +
        "that travel through the quay will still be permitted."
      )
    )
    .field(
      GqlUtil.newIdListInputField(
        "quaysHard",
        "NOT IMPLEMENTED. Set of ids of quays that should not be allowed for boarding, alighting or traveling " +
        "thorugh."
      )
    )
    .field(
      GqlUtil.newIdListInputField(
        "serviceJourneys",
        "Set of ids of service journeys that should not be used."
      )
    )
    .field(
      GqlUtil.newIdListInputField(
        "rentalNetworks",
        "Set of ids of rental networks that should not be allowed for renting vehicles."
      )
    )
    .build();
}

package org.opentripplanner.apis.transmodel.model.plan;

import static org.opentripplanner.apis.transmodel.support.GqlUtil.newIdListInputField;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLInputObjectType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.core.model.id.FeedScopedId;

public class JourneyWhiteListed {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("InputWhiteListed")
    .description(
      "Filter trips by only allowing lines involving certain " +
        "elements. If both lines and authorities are specified, only one must be valid " +
        "for each line to be used. If a line is both banned and whitelisted, it will " +
        "be counted as banned."
    )
    .field(newIdListInputField("lines", "Set of ids for lines that should be used"))
    .field(newIdListInputField("authorities", "Set of ids for authorities that should be used"))
    .field(
      GqlUtil.newIdListInputField(
        "rentalNetworks",
        "Set of ids of rental networks that should be used for renting vehicles."
      )
    )
    .build();

  public final Set<FeedScopedId> authorityIds;
  public final Set<FeedScopedId> lineIds;

  public JourneyWhiteListed(DataFetchingEnvironment environment, FeedScopedIdMapper idMapper) {
    Map<String, List<String>> whiteList = environment.getArgument("whiteListed");
    if (whiteList == null) {
      this.authorityIds = Set.of();
      this.lineIds = Set.of();
    } else {
      this.authorityIds = Set.copyOf(idMapper.parseListNullSafe(whiteList.get("authorities")));
      this.lineIds = Set.copyOf(idMapper.parseListNullSafe(whiteList.get("lines")));
    }
  }
}

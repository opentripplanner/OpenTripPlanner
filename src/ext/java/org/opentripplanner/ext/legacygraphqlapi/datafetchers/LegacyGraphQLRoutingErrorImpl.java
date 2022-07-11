package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.opentripplanner.ext.legacygraphqlapi.LegacyGraphQLUtils.toGraphQL;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.routing.api.response.RoutingError;

public class LegacyGraphQLRoutingErrorImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLRoutingError {

  @Override
  public DataFetcher<LegacyGraphQLTypes.LegacyGraphQLRoutingErrorCode> code() {
    return environment -> toGraphQL(getSource(environment).code);
  }

  @Override
  public DataFetcher<String> description() {
    return environment ->
      PlannerErrorMapper.mapMessage(getSource(environment)).message.get(environment.getLocale());
  }

  @Override
  public DataFetcher<LegacyGraphQLTypes.LegacyGraphQLInputField> inputField() {
    return environment -> toGraphQL(getSource(environment).inputField);
  }

  private RoutingError getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

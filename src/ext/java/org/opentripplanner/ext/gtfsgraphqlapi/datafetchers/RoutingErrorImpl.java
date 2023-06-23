package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import static org.opentripplanner.ext.gtfsgraphqlapi.GraphQLUtils.toGraphQL;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.routing.api.response.RoutingError;

public class RoutingErrorImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLRoutingError {

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

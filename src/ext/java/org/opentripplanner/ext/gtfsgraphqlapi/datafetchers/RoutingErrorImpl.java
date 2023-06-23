package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import static org.opentripplanner.ext.gtfsgraphqlapi.GraphQLUtils.toGraphQL;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes;
import org.opentripplanner.routing.api.response.RoutingError;

public class RoutingErrorImpl implements GraphQLDataFetchers.GraphQLRoutingError {

  @Override
  public DataFetcher<GraphQLTypes.GraphQLRoutingErrorCode> code() {
    return environment -> toGraphQL(getSource(environment).code);
  }

  @Override
  public DataFetcher<String> description() {
    return environment ->
      PlannerErrorMapper.mapMessage(getSource(environment)).message.get(environment.getLocale());
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLInputField> inputField() {
    return environment -> toGraphQL(getSource(environment).inputField);
  }

  private RoutingError getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

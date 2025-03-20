package org.opentripplanner.apis.gtfs.datafetchers;

import static org.opentripplanner.apis.gtfs.GraphQLUtils.toGraphQL;
import static org.opentripplanner.framework.graphql.GraphQLUtils.getLocale;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.mapping.PlannerErrorMapper;
import org.opentripplanner.routing.api.response.RoutingError;

public class RoutingErrorImpl implements GraphQLDataFetchers.GraphQLRoutingError {

  @Override
  public DataFetcher<GraphQLTypes.GraphQLRoutingErrorCode> code() {
    return environment -> toGraphQL(getSource(environment).code);
  }

  @Override
  public DataFetcher<String> description() {
    return environment ->
      PlannerErrorMapper.mapMessage(getSource(environment)).message.get(getLocale(environment));
  }

  @Override
  public DataFetcher<GraphQLTypes.GraphQLInputField> inputField() {
    return environment -> toGraphQL(getSource(environment).inputField);
  }

  private RoutingError getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

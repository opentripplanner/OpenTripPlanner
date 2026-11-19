package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.DestinationDisplay;
import org.opentripplanner.framework.graphql.GraphQLUtils;

public class DestinationDisplayImpl implements GraphQLDataFetchers.GraphQLDestinationDisplay {

  @Override
  public DataFetcher<String> headsign() {
    return environment ->
      GraphQLUtils.getTranslation(getSource(environment).headsign(), environment);
  }

  private DestinationDisplay getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

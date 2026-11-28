package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.site.FareArea;

public class AreaImpl implements GraphQLDataFetchers.GraphQLArea {

  @Override
  public DataFetcher<String> gtfsId() {
    return env -> source(env).getId().toString();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> source(env).getName();
  }

  private static FareArea source(DataFetchingEnvironment env) {
    return env.getSource();
  }
}

package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.fare.RiderCategory;

public class RiderCategoryImpl implements GraphQLDataFetchers.GraphQLRiderCategory {

  @Override
  public DataFetcher<String> id() {
    return env -> source(env).id().toString();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> source(env).name();
  }

  @Override
  public DataFetcher<Boolean> isDefault() {
    return env -> source(env).isDefault();
  }

  private static RiderCategory source(DataFetchingEnvironment env) {
    return env.getSource();
  }
}

package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.site.AreaStop;

public class LocationImpl implements GraphQLDataFetchers.GraphQLLocation {

  @Override
  public DataFetcher<Object> geometry() {
    return env -> source(env).getGeometry();
  }

  @Override
  public DataFetcher<String> id() {
    return env -> source(env).getId().toString();
  }

  @Override
  public DataFetcher<String> name() {
    return env ->
      org.opentripplanner.framework.graphql.GraphQLUtils.getTranslation(source(env).getName(), env);
  }

  private AreaStop source(DataFetchingEnvironment env) {
    return env.getSource();
  }
}

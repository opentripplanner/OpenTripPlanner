package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.transit.model.site.GroupStop;

public class LocationGroupImpl implements GraphQLDataFetchers.GraphQLLocationGroup {

  @Override
  public DataFetcher<String> gtfsId() {
    return env -> source(env).getId().toString();
  }

  @Override
  public DataFetcher<Iterable<Object>> members() {
    return env -> {
      var stop = source(env);
      // not sure why the List.copy is necessary, but without it the compiler complains
      return List.copyOf(stop.getChildLocations());
    };
  }

  @Override
  public DataFetcher<String> name() {
    return env -> GraphQLUtils.getTranslation(source(env).getName(), env);
  }

  private GroupStop source(DataFetchingEnvironment env) {
    return env.getSource();
  }
}

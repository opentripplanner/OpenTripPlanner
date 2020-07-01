package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLelevationProfileComponentImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLElevationProfileComponent {

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).first;
  }

  @Override
  public DataFetcher<Double> elevation() {
    return environment -> getSource(environment).second;
  }

  private P2<Double> getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

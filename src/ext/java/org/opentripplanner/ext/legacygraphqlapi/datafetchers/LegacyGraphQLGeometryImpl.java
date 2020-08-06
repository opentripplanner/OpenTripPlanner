package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class LegacyGraphQLGeometryImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLGeometry {

  @Override
  public DataFetcher<Integer> length() {
    return environment -> getSource(environment).getLength();
  }

  @Override
  public DataFetcher<String> points() {
    return environment -> getSource(environment).getPoints();
  }

  private EncodedPolylineBean getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

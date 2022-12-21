package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.framework.geometry.EncodedPolyline;

public class LegacyGraphQLGeometryImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLGeometry {

  @Override
  public DataFetcher<Integer> length() {
    return environment -> getSource(environment).length();
  }

  @Override
  public DataFetcher<String> points() {
    return environment -> getSource(environment).points();
  }

  private EncodedPolyline getSource(DataFetchingEnvironment environment) {
    return EncodedPolyline.encode((Geometry) environment.getSource());
  }
}

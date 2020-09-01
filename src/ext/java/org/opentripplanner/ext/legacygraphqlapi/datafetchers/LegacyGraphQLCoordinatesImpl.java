package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.locationtech.jts.geom.Coordinate;

public class LegacyGraphQLCoordinatesImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLCoordinates {

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getY();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getX();
  }

  private Coordinate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

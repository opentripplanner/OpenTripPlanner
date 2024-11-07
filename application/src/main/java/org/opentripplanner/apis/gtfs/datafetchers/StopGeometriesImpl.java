package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers.GraphQLStopGeometries;

public class StopGeometriesImpl implements GraphQLStopGeometries {

  @Override
  public DataFetcher<Geometry> geoJson() {
    return this::getSource;
  }

  @Override
  public DataFetcher<Iterable<Geometry>> googleEncoded() {
    return env -> {
      Geometry geometries = getSource(env);
      ArrayList<Geometry> output = new ArrayList<>();

      for (int i = 0; i < geometries.getNumGeometries(); i++) {
        output.add(geometries.getGeometryN(i));
      }
      return output;
    };
  }

  private Geometry getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

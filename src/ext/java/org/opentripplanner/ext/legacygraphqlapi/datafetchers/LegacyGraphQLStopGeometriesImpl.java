package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLStopGeometries;

import java.util.ArrayList;

public class LegacyGraphQLStopGeometriesImpl implements LegacyGraphQLStopGeometries {

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

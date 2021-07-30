package org.opentripplanner.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.common.geometry.GeometryUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GeoJsonUtils {
    // Create a precision reducer that can be used to reduce geometry precision in case JTS doesn't feel like dealing
    // with large amounts of coordinate precision. This will round the geometry precision to a maximum of 7 significant
    // digits.
    private static GeometryPrecisionReducer precisionReducer = new GeometryPrecisionReducer(
        new PrecisionModel(1_000_000)
    );

    public static Geometry parsePolygonOrMultiPolygonFromJsonNode(
        JsonNode jsonNode
    ) throws UnsupportedGeometryException, IOException {
        ObjectMapper jsonDeserializer = new ObjectMapper();

        // first try to deserialize as a feature
        try {
            Feature geoJsonFeature = jsonDeserializer.readValue(
                jsonNode.traverse(),
                Feature.class
            );
            GeoJsonObject geometry = geoJsonFeature.getGeometry();
            return GeometryUtils.convertGeoJsonToJtsGeometry(geometry);
        } catch (IllegalArgumentException | IOException | UnsupportedGeometryException e) {
            // Could not parse as a Feature, trying as a FeatureCollection
                List<Geometry> geometries = new ArrayList<>();
            FeatureCollection geoJsonFeatureCollection = jsonDeserializer.readValue(
                jsonNode.traverse(),
                FeatureCollection.class
            );

            // convert all features to geometry
            for (Feature feature : geoJsonFeatureCollection.getFeatures()) {
                geometries.add(GeometryUtils.convertGeoJsonToJtsGeometry(feature.getGeometry()));
            }

            // if there is just one geometry item, return that immediately, otherwise a ClassCastException will occur
            if (geometries.size() == 1) {
                return geometries.get(0);
            }

            // union all geometries into a single geometry
            GeometryFactory geometryFactory = new GeometryFactory();
            GeometryCollection geometryCollection =
                (GeometryCollection) geometryFactory.buildGeometry(geometries);
            try {
                return geometryCollection.union();
            } catch (TopologyException topologyException) {
                // Sometimes JTS can fail with valid geometries. Retry with reduced precision and a 0 buffer addition if
                // a TopologyException occurs.
                // See https://github.com/locationtech/jts/issues/120
                // See https://github.com/locationtech/jts/issues/511
                geometryCollection = (GeometryCollection) precisionReducer.reduce(geometryCollection);
                GeometryCollection bufferedGeometryCollection = (GeometryCollection) geometryCollection.buffer(0);
                return bufferedGeometryCollection.union();
            }
        }
    }
}

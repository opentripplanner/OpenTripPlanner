package org.opentripplanner.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.common.geometry.GeometryUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GeoJsonUtils {
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

            // union all geometries into a single geometry
            GeometryFactory geometryFactory = new GeometryFactory();
            GeometryCollection geometryCollection =
                (GeometryCollection) geometryFactory.buildGeometry(geometries);
            return geometryCollection.union();
        }
    }
}

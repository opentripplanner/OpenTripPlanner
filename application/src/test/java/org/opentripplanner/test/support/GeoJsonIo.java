package org.opentripplanner.test.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Helper class for generating URLs to geojson.io.
 */
public class GeoJsonIo {

  private static final ObjectMapper MAPPER = ObjectMappers.geoJson();

  public static String toUrl(Geometry geometry) {
    try {
      var geoJson = MAPPER.writeValueAsString(geometry);
      var encoded = URLEncoder.encode(geoJson, StandardCharsets.UTF_8);
      return "http://geojson.io/#data=data:application/json,%s".formatted(encoded);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String toUrl(Graph graph) {
    var geometries = graph.getEdges().stream().map(Edge::getGeometry).toList();
    var geomArray = GeometryFactory.toGeometryArray(geometries);

    var collection = GeometryUtils.getGeometryFactory().createGeometryCollection(geomArray);
    return toUrl(collection);
  }
}

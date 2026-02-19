package org.opentripplanner.test.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.collection.ListUtils;

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
    return toUrl(graph.getEdges(), graph.getVertices());
  }

  public static String toUrl(Collection<Edge> edges, Collection<Vertex> vertices) {
    var edgeGeoms = edges.stream().map(GeoJsonIo::geom).toList();
    var vertexGeoms = vertices
      .stream()
      .map(v -> GeometryUtils.getGeometryFactory().createPoint(v.getCoordinate()))
      .map(Geometry.class::cast)
      .toList();
    var geomArray = GeometryFactory.toGeometryArray(ListUtils.combine(edgeGeoms, vertexGeoms));
    var collection = GeometryUtils.getGeometryFactory().createGeometryCollection(geomArray);
    return toUrl(collection);
  }

  private static Geometry geom(Edge e) {
    return Objects.requireNonNullElse(
      e.getGeometry(),
      GeometryUtils.makeLineString(
        e.getFromVertex().getCoordinate(),
        e.getToVertex().getCoordinate()
      )
    );
  }
}

package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.apis.vectortiles.DebugStyleSpec.VectorSourceLayer;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Builds a Maplibre/Mapbox <a href="https://maplibre.org/maplibre-style-spec/layers/">vector tile
 * layer style</a>.
 */
public class LayerStyleBuilder {

  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  private static final String TYPE = "type";
  private static final String SOURCE_LAYER = "source-layer";
  private final Map<String, Object> props = new LinkedHashMap<>();
  private final Map<String, Object> paint = new LinkedHashMap<>();
  private final Map<String, Object> layout = new LinkedHashMap<>();
  private final Map<String, Object> line = new LinkedHashMap<>();
  private List<String> filter = List.of();

  public static LayerStyleBuilder ofId(String id) {
    return new LayerStyleBuilder(id);
  }

  public LayerStyleBuilder vectorSourceLayer(VectorSourceLayer source) {
    source(source.vectorSource());
    return sourceLayer(source.vectorLayer());
  }



  public enum LayerType {
    Circle,
    Line,
    Raster,
  }

  private LayerStyleBuilder(String id) {
    props.put("id", id);
  }

  public LayerStyleBuilder minZoom(int i) {
    props.put("minzoom", i);
    return this;
  }

  public LayerStyleBuilder maxZoom(int i) {
    props.put("maxzoom", i);
    return this;
  }

  /**
   * Which vector tile source this should apply to.
   */
  public LayerStyleBuilder source(TileSource source) {
    props.put("source", source.id());
    return this;
  }

  /**
   * For vector tile sources, specify which source layer in the tile the styles should apply to.
   * There is an unfortunate collision in the name "layer" as it can both refer to a styling layer
   * and the layer inside the vector tile.
   */
  public LayerStyleBuilder sourceLayer(String source) {
    props.put(SOURCE_LAYER, source);
    return this;
  }

  public LayerStyleBuilder typeRaster() {
    return type(LayerType.Raster);
  }

  public LayerStyleBuilder typeCircle() {
    return type(LayerType.Circle);
  }

  public LayerStyleBuilder typeLine() {
    type(LayerType.Line);
    line.put("line-cap", "round");
    return this;
  }

  private LayerStyleBuilder type(LayerType type) {
    props.put(TYPE, type.name().toLowerCase());
    return this;
  }

  public LayerStyleBuilder circleColor(String color) {
    paint.put("circle-color", validateColor(color));
    return this;
  }

  public LayerStyleBuilder circleStroke(String color, int width) {
    paint.put("circle-stroke-color", validateColor(color));
    paint.put("circle-stroke-width", width);
    return this;
  }

  public LayerStyleBuilder circleRadius(ZoomDependentNumber radius) {
    paint.put("circle-radius", radius.toJson());
    return this;
  }

  // Line styling

  public LayerStyleBuilder lineColor(String color) {
    paint.put("line-color", validateColor(color));
    return this;
  }

  public LayerStyleBuilder lineWidth(float width) {
    paint.put("line-width", width);
    return this;
  }

  public LayerStyleBuilder lineWidth(ZoomDependentNumber zoomStops) {
    paint.put("line-width", zoomStops.toJson());
    return this;
  }

  public LayerStyleBuilder intiallyHidden() {
    layout.put("visibility", "none");
    return this;
  }

  // filtering edge
  @SafeVarargs
  public final LayerStyleBuilder edgeFilter(Class<? extends Edge>... classToFilter) {
    var clazzes = Arrays.stream(classToFilter).map(Class::getSimpleName).toList();
    filter = ListUtils.combine(List.of("in", "class"), clazzes);
    return this;
  }

  public JsonNode toJson() {
    validate();

    var copy = new LinkedHashMap<>(props);
    if (!paint.isEmpty()) {
      copy.put("paint", paint);
    }
    if (!filter.isEmpty()) {
      copy.put("filter", filter);
    }
    if (!layout.isEmpty()) {
      copy.put("layout", layout);
    }
    if (!line.isEmpty()) {
      copy.put("line", line);
    }
    return OBJECT_MAPPER.valueToTree(copy);
  }

  private String validateColor(String color) {
    if (!color.startsWith("#")) {
      throw new IllegalArgumentException("Colors must start with '#'");
    }
    return color;
  }

  private void validate() {
    Stream
      .of(TYPE)
      .forEach(p -> Objects.requireNonNull(props.get(p), "%s must be set".formatted(p)));
  }

  public record ZoomStop(int zoom, float value) {
    public List<Number> toList() {
      return List.of(zoom, value);
    }
  }

  public record ZoomDependentNumber(float base, List<ZoomStop> stops) {
    public JsonNode toJson() {
      var props = new LinkedHashMap<>();
      props.put("base", base);
      var vals = stops.stream().map(ZoomStop::toList).toList();
      props.put("stops", vals);
      return OBJECT_MAPPER.valueToTree(props);
    }
  }
}

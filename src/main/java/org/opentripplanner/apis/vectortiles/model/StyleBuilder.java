package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Builds a Maplibre/Mapbox <a href="https://maplibre.org/maplibre-style-spec/layers/">vector tile
 * layer style</a>.
 */
public class StyleBuilder {

  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  private static final String TYPE = "type";
  private static final String SOURCE_LAYER = "source-layer";
  private final Map<String, Object> props = new LinkedHashMap<>();
  private final Map<String, Object> paint = new LinkedHashMap<>();
  private final Map<String, Object> layout = new LinkedHashMap<>();
  private final Map<String, Object> line = new LinkedHashMap<>();
  private List<String> filter = List.of();

  public static StyleBuilder ofId(String id) {
    return new StyleBuilder(id);
  }

  public StyleBuilder vectorSourceLayer(VectorSourceLayer source) {
    source(source.vectorSource());
    return sourceLayer(source.vectorLayer());
  }

  public enum LayerType {
    Circle,
    Line,
    Raster,
    Fill,
  }

  private StyleBuilder(String id) {
    props.put("id", id);
  }

  public StyleBuilder minZoom(int i) {
    props.put("minzoom", i);
    return this;
  }

  public StyleBuilder maxZoom(int i) {
    props.put("maxzoom", i);
    return this;
  }

  /**
   * Which vector tile source this should apply to.
   */
  public StyleBuilder source(TileSource source) {
    props.put("source", source.id());
    return this;
  }

  /**
   * For vector tile sources, specify which source layer in the tile the styles should apply to.
   * There is an unfortunate collision in the name "layer" as it can both refer to a styling layer
   * and the layer inside the vector tile.
   */
  public StyleBuilder sourceLayer(String source) {
    props.put(SOURCE_LAYER, source);
    return this;
  }

  public StyleBuilder typeRaster() {
    return type(LayerType.Raster);
  }

  public StyleBuilder typeCircle() {
    return type(LayerType.Circle);
  }

  public StyleBuilder typeLine() {
    type(LayerType.Line);
    layout.put("line-cap", "round");
    return this;
  }

  public StyleBuilder typeFill() {
    type(LayerType.Fill);
    return this;
  }

  private StyleBuilder type(LayerType type) {
    props.put(TYPE, type.name().toLowerCase());
    return this;
  }

  public StyleBuilder circleColor(String color) {
    paint.put("circle-color", validateColor(color));
    return this;
  }

  public StyleBuilder circleStroke(String color, int width) {
    paint.put("circle-stroke-color", validateColor(color));
    paint.put("circle-stroke-width", width);
    return this;
  }

  public StyleBuilder circleStroke(String color, ZoomDependentNumber width) {
    paint.put("circle-stroke-color", validateColor(color));
    paint.put("circle-stroke-width", width.toJson());
    return this;
  }

  public StyleBuilder circleRadius(ZoomDependentNumber radius) {
    paint.put("circle-radius", radius.toJson());
    return this;
  }

  // Line styling

  public StyleBuilder lineColor(String color) {
    paint.put("line-color", validateColor(color));
    return this;
  }

  public StyleBuilder lineWidth(float width) {
    paint.put("line-width", width);
    return this;
  }

  public StyleBuilder lineWidth(ZoomDependentNumber zoomStops) {
    paint.put("line-width", zoomStops.toJson());
    return this;
  }

  public StyleBuilder fillColor(String color) {
    paint.put("fill-color", validateColor(color));
    return this;
  }

  public StyleBuilder fillOpacity(float opacity) {
    paint.put("fill-opacity", opacity);
    return this;
  }

  public StyleBuilder fillOutlineColor(String color) {
    paint.put("fill-outline-color", validateColor(color));
    return this;
  }

  /**
   * Hide this layer when the debug client starts. It can be made visible in the UI later.
   */
  public StyleBuilder intiallyHidden() {
    layout.put("visibility", "none");
    return this;
  }

  /**
   * Only apply the style to the given edges.
   */
  @SafeVarargs
  public final StyleBuilder edgeFilter(Class<? extends Edge>... classToFilter) {
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
}

package org.opentripplanner.apis.vectortiles.model;

import static org.opentripplanner.utils.lang.DoubleUtils.roundTo2Decimals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.apis.vectortiles.model.ZoomDependentNumber.ZoomStop;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.collection.ListUtils;

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
  private final Map<String, Object> metadata = new LinkedHashMap<>();
  private final Map<String, Object> line = new LinkedHashMap<>();
  private List<?> filter = List.of();

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
    Symbol,
  }

  private StyleBuilder(String id) {
    props.put("id", id);
    metadata.put("group", "Other");
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

  public StyleBuilder typeSymbol() {
    type(LayerType.Symbol);
    return this;
  }

  private StyleBuilder type(LayerType type) {
    props.put(TYPE, type.name().toLowerCase());
    return this;
  }

  /**
   * Puts the layer into an arbitrarily defined group in the layer selector. This allows you
   * to switch the entire group on and off.
   */
  public StyleBuilder group(String group) {
    metadata.put("group", group);
    return this;
  }

  /**
   * A nice human-readable name for the layer.
   */
  public StyleBuilder displayName(String name) {
    metadata.put("name", name);
    return this;
  }

  public StyleBuilder symbolText(String name) {
    layout.put("text-field", "{%s}".formatted(name));
    layout.put("text-font", List.of("KlokanTech Noto Sans Regular"));
    layout.put(
      "text-size",
      new ZoomDependentNumber(List.of(new ZoomStop(10, 6), new ZoomStop(24, 12))).toJson()
    );
    layout.put("text-max-width", 100);
    layout.put("text-keep-upright", true);
    layout.put("text-rotation-alignment", "map");
    layout.put("text-overlap", "never");
    paint.put("text-color", "#000");
    paint.put("text-halo-color", "#fff");
    paint.put("text-halo-blur", 4);
    paint.put("text-halo-width", 3);
    return this;
  }

  public StyleBuilder lineText(String name) {
    layout.put("symbol-placement", "line-center");
    layout.put("symbol-spacing", 1000);
    return symbolText(name);
  }

  public StyleBuilder textOffset(float offset) {
    layout.put("text-offset", List.of(0, offset));
    return this;
  }

  public StyleBuilder circleColor(String color) {
    paint.put("circle-color", validateColor(color));
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
  public StyleBuilder lineCap(String lineCap) {
    layout.put("line-cap", lineCap);
    return this;
  }

  public StyleBuilder lineColor(String color) {
    paint.put("line-color", validateColor(color));
    return this;
  }

  /**
   * Generates the line color based off a numeric property in the feature.
   * <p>
   * The scale of the property must be between 0 and infinity but the color scale is limited to be
   * between minValue and maxValue.
   * <p>
   * minValue is displayed as a bright green and the higher the number gets, the "redder" the color
   * becomes.
   */
  public StyleBuilder lineColorFromProperty(String propertyName, double minValue, double maxValue) {
    var multiplier = List.of(
      "*",
      roundTo2Decimals(255 / (maxValue - minValue)),
      List.of("get", propertyName)
    );
    setLineColor(multiplier);
    return this;
  }

  /**
   * Generates the line color based off a numeric property in the feature.
   * <p>
   * The scale of the property must be between 1 and infinity. RGB values (0, 255) are computed with
   * the following formula: log2(propertyValue) * logMultiplier.
   * <p>
   * 1 is displayed as a bright green and the higher the number gets, the "redder" the color
   * becomes.
   */
  public StyleBuilder log2LineColorFromProperty(String propertyName, double logMultiplier) {
    var multiplier = List.of("*", logMultiplier, List.of("log2", List.of("get", propertyName)));
    setLineColor(multiplier);
    return this;
  }

  public StyleBuilder lineColorMatch(
    String propertyName,
    Collection<String> values,
    String defaultValue
  ) {
    paint.put(
      "line-color",
      ListUtils.combine(
        List.of("match", List.of("get", propertyName)),
        (Collection) values,
        List.of(defaultValue)
      )
    );
    return this;
  }

  public StyleBuilder lineWidth(ZoomDependentNumber zoomStops) {
    paint.put("line-width", zoomStops.toJson());
    return this;
  }

  public StyleBuilder lineOffset(ZoomDependentNumber zoomStops) {
    paint.put("line-offset", zoomStops.toJson());
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
    return filterClasses(classToFilter);
  }

  /**
   * Filter the entities by a boolean property.
   */
  public final StyleBuilder booleanFilter(String propertyName, boolean value) {
    filter = List.of("==", propertyName, value);
    return this;
  }

  /**
   * Only apply the style to the given vertices.
   */
  @SafeVarargs
  public final StyleBuilder vertexFilter(Class<? extends Vertex>... classToFilter) {
    return filterClasses(classToFilter);
  }

  @SafeVarargs
  public final StyleBuilder classFilter(Class<?>... classToFilter) {
    return filterClasses(classToFilter);
  }

  public StyleBuilder filterValueInProperty(String propertyName, String... values) {
    var newFilter = new ArrayList<>();
    newFilter.add("any");
    for (String value : values) {
      newFilter.add(List.of("in", value, List.of("string", List.of("get", propertyName))));
    }
    filter = newFilter;
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
    copy.put("metadata", metadata);
    return OBJECT_MAPPER.valueToTree(copy);
  }

  private StyleBuilder filterClasses(Class... classToFilter) {
    var clazzes = Arrays.stream(classToFilter).map(Class::getSimpleName).toList();
    filter = new ArrayList<>(ListUtils.combine(List.of("in", "class"), clazzes));
    return this;
  }

  private String validateColor(String color) {
    if (!color.startsWith("#")) {
      throw new IllegalArgumentException("Colors must start with '#'");
    }
    return color;
  }

  private void validate() {
    Stream.of(TYPE).forEach(p ->
      Objects.requireNonNull(props.get(p), "%s must be set".formatted(p))
    );
  }

  private void setLineColor(List<Object> valueSpecifier) {
    paint.put(
      "line-color",
      List.of(
        "rgb",
        List.of("min", 255, valueSpecifier),
        List.of("max", 0, List.of("-", 255, valueSpecifier)),
        // we add a small amount of blue so that the colours don't look too neon
        60
      )
    );
  }
}

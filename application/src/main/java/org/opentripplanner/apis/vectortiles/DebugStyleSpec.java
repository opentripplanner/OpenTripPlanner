package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.inspector.vector.edge.EdgePropertyMapper.streetPermissionAsString;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.apis.vectortiles.model.StyleBuilder;
import org.opentripplanner.apis.vectortiles.model.StyleSpec;
import org.opentripplanner.apis.vectortiles.model.TileSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.RasterSource;
import org.opentripplanner.apis.vectortiles.model.VectorSourceLayer;
import org.opentripplanner.apis.vectortiles.model.ZoomDependentNumber;
import org.opentripplanner.apis.vectortiles.model.ZoomDependentNumber.ZoomStop;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.standalone.config.debuguiconfig.BackgroundTileLayer;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetStationCentroidLink;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * A Mapbox/Mapblibre style specification for rendering debug information about transit and street
 * data.
 */
public class DebugStyleSpec {

  private static final TileSource OSM_BACKGROUND = new RasterSource(
    "OSM Carto",
    List.of("https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"),
    19,
    256,
    "© OpenStreetMap Contributors"
  );
  private static final TileSource POSITRON_BACKGROUND = new RasterSource(
    "Positron",
    List.of("https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/{z}/{x}/{y}{ratio}.png"),
    19,
    256,
    "© <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>, &copy; <a href=\"https://carto.com/attributions\">CARTO</a>"
  );

  private static final List<TileSource> BACKGROUND_LAYERS = List.of(
    OSM_BACKGROUND,
    POSITRON_BACKGROUND
  );
  private static final String MAGENTA = "#f21d52";
  private static final String BRIGHT_GREEN = "#22DD9E";
  private static final String DARK_GREEN = "#136b04";
  private static final String RED = "#fc0f2a";
  private static final String PURPLE = "#BC55F2";
  private static final String BLACK = "#140d0e";

  private static final int MAX_ZOOM = 23;
  private static final int LINE_DETAIL_ZOOM = 13;
  private static final ZoomDependentNumber LINE_OFFSET = new ZoomDependentNumber(
    List.of(new ZoomStop(LINE_DETAIL_ZOOM, 0.4f), new ZoomStop(MAX_ZOOM, 7))
  );
  private static final ZoomDependentNumber LINE_WIDTH = new ZoomDependentNumber(
    List.of(new ZoomStop(LINE_DETAIL_ZOOM, 0.2f), new ZoomStop(MAX_ZOOM, 8))
  );
  private static final ZoomDependentNumber LINE_HALF_WIDTH = new ZoomDependentNumber(
    List.of(new ZoomStop(LINE_DETAIL_ZOOM, 0.1f), new ZoomStop(MAX_ZOOM, 6))
  );
  private static final ZoomDependentNumber CIRCLE_STROKE = new ZoomDependentNumber(
    List.of(new ZoomStop(15, 0.2f), new ZoomStop(MAX_ZOOM, 3))
  );
  private static final Class<Edge>[] EDGES_TO_DISPLAY = new Class[] {
    StreetEdge.class,
    AreaEdge.class,
    EscalatorEdge.class,
    PathwayEdge.class,
    ElevatorHopEdge.class,
    TemporaryPartialStreetEdge.class,
    TemporaryFreeEdge.class,
  };
  private static final String EDGES_GROUP = "Edges";
  private static final String ELEVATION_GROUP = "Elevation";
  private static final String WALK_SAFETY_GROUP = "Walk safety";
  private static final String BICYCLE_SAFETY_GROUP = "Bicycle safety";
  private static final String STOPS_GROUP = "Stops";
  private static final String VERTICES_GROUP = "Vertices";
  private static final String PERMISSIONS_GROUP = "Permissions";
  private static final String NO_THRU_TRAFFIC_GROUP = "No-thru traffic";

  private static final StreetTraversalPermission[] streetModes = new StreetTraversalPermission[] {
    StreetTraversalPermission.PEDESTRIAN,
    StreetTraversalPermission.BICYCLE,
    StreetTraversalPermission.CAR,
  };
  private static final String WHEELCHAIR_GROUP = "Wheelchair accessibility";

  static StyleSpec build(
    VectorSourceLayer regularStops,
    VectorSourceLayer areaStops,
    VectorSourceLayer groupStops,
    VectorSourceLayer edges,
    VectorSourceLayer vertices,
    List<BackgroundTileLayer> extraLayers
  ) {
    List<TileSource> vectorSources = Stream.of(regularStops, edges, vertices)
      .map(VectorSourceLayer::vectorSource)
      .map(TileSource.class::cast)
      .toList();

    List<TileSource> extraRasterSources = extraLayers
      .stream()
      .map(l ->
        (TileSource) new RasterSource(
          l.name(),
          List.of(l.templateUrl()),
          19,
          l.tileSize(),
          l.attribution()
        )
      )
      .toList();
    var allSources = ListUtils.combine(BACKGROUND_LAYERS, extraRasterSources, vectorSources);
    return new StyleSpec(
      "OTP Debug Tiles",
      allSources,
      ListUtils.combine(
        backgroundLayers(extraRasterSources),
        wheelchair(edges),
        noThruTraffic(edges),
        bicycleSafety(edges),
        walkSafety(edges),
        traversalPermissions(edges),
        edges(edges),
        elevation(edges, vertices),
        vertices(vertices),
        stops(regularStops, areaStops, groupStops)
      )
    );
  }

  private static List<StyleBuilder> backgroundLayers(List<TileSource> extraLayers) {
    return ListUtils.combine(BACKGROUND_LAYERS, extraLayers)
      .stream()
      .map(layer -> {
        var builder = StyleBuilder.ofId(layer.id())
          .displayName(layer.name())
          .typeRaster()
          .source(layer)
          .minZoom(0);
        if (!layer.equals(OSM_BACKGROUND)) {
          builder.intiallyHidden();
        }
        return builder;
      })
      .toList();
  }

  private static List<StyleBuilder> stops(
    VectorSourceLayer regularStops,
    VectorSourceLayer areaStops,
    VectorSourceLayer groupStops
  ) {
    return List.of(
      StyleBuilder.ofId("area-stop")
        .group(STOPS_GROUP)
        .typeFill()
        .vectorSourceLayer(areaStops)
        .fillColor(BRIGHT_GREEN)
        .fillOpacity(0.5f)
        .fillOutlineColor(BLACK)
        .minZoom(6)
        .maxZoom(MAX_ZOOM),
      StyleBuilder.ofId("group-stop")
        .group(STOPS_GROUP)
        .typeFill()
        .vectorSourceLayer(groupStops)
        .fillColor(BRIGHT_GREEN)
        .fillOpacity(0.5f)
        .fillOutlineColor(BLACK)
        .minZoom(6)
        .maxZoom(MAX_ZOOM),
      StyleBuilder.ofId("regular-stop")
        .group(STOPS_GROUP)
        .typeCircle()
        .vectorSourceLayer(regularStops)
        .circleStroke(
          BLACK,
          new ZoomDependentNumber(List.of(new ZoomStop(11, 0.5f), new ZoomStop(MAX_ZOOM, 5)))
        )
        .circleRadius(
          new ZoomDependentNumber(List.of(new ZoomStop(11, 0.5f), new ZoomStop(MAX_ZOOM, 10)))
        )
        .circleColor("#fcf9fa")
        .minZoom(10)
        .maxZoom(MAX_ZOOM)
    );
  }

  private static List<StyleBuilder> vertices(VectorSourceLayer vertices) {
    return List.of(
      StyleBuilder.ofId("vertex")
        .group(VERTICES_GROUP)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(
          new ZoomDependentNumber(List.of(new ZoomStop(15, 1), new ZoomStop(MAX_ZOOM, 7)))
        )
        .circleColor(PURPLE)
        .minZoom(15)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("parking-vertex")
        .group(VERTICES_GROUP)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .vertexFilter(VehicleParkingEntranceVertex.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(
          new ZoomDependentNumber(List.of(new ZoomStop(13, 1.4f), new ZoomStop(MAX_ZOOM, 10)))
        )
        .circleColor(DARK_GREEN)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> edges(VectorSourceLayer edges) {
    return List.of(
      StyleBuilder.ofId("edge")
        .group(EDGES_GROUP)
        .typeLine()
        .vectorSourceLayer(edges)
        .lineColor(MAGENTA)
        .edgeFilter(EDGES_TO_DISPLAY)
        .lineWidth(LINE_HALF_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("edge-name")
        .group(EDGES_GROUP)
        .typeSymbol()
        .lineText("name")
        .vectorSourceLayer(edges)
        .edgeFilter(EDGES_TO_DISPLAY)
        .minZoom(17)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("link")
        .group(EDGES_GROUP)
        .typeLine()
        .vectorSourceLayer(edges)
        .lineColor(BRIGHT_GREEN)
        .edgeFilter(
          StreetTransitStopLink.class,
          StreetTransitEntranceLink.class,
          BoardingLocationToStopLink.class,
          StreetVehicleRentalLink.class,
          StreetVehicleParkingLink.class,
          StreetStationCentroidLink.class
        )
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> elevation(VectorSourceLayer edges, VectorSourceLayer vertices) {
    return List.of(
      StyleBuilder.ofId("maximum-slope")
        .group(ELEVATION_GROUP)
        .typeLine()
        .vectorSourceLayer(edges)
        // Slope can be higher than this in theory but distinction between high values is not needed
        .lineColorFromProperty("maximumSlope", 0, 0.35)
        .edgeFilter(StreetEdge.class)
        .lineWidth(LINE_HALF_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("vertex-elevation")
        .group(ELEVATION_GROUP)
        .typeSymbol()
        .symbolText("elevation")
        .vectorSourceLayer(vertices)
        .minZoom(17)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> bicycleSafety(VectorSourceLayer edges) {
    return List.of(
      StyleBuilder.ofId("bicycle-safety")
        .group(BICYCLE_SAFETY_GROUP)
        .typeLine()
        .vectorSourceLayer(edges)
        .log2LineColorFromProperty("bicycleSafetyFactor", 80)
        .edgeFilter(StreetEdge.class)
        .lineWidth(LINE_HALF_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("bicycle-safety-text")
        .vectorSourceLayer(edges)
        .group(BICYCLE_SAFETY_GROUP)
        .typeSymbol()
        .lineText("bicycleSafetyFactor")
        .textOffset(1)
        .edgeFilter(EDGES_TO_DISPLAY)
        .minZoom(17)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> walkSafety(VectorSourceLayer edges) {
    return List.of(
      StyleBuilder.ofId("walk-safety")
        .group(WALK_SAFETY_GROUP)
        .typeLine()
        .vectorSourceLayer(edges)
        .log2LineColorFromProperty("walkSafetyFactor", 80)
        .edgeFilter(StreetEdge.class)
        .lineWidth(LINE_HALF_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("walk-safety-text")
        .vectorSourceLayer(edges)
        .group(WALK_SAFETY_GROUP)
        .typeSymbol()
        .lineText("walkSafetyFactor")
        .textOffset(1)
        .edgeFilter(EDGES_TO_DISPLAY)
        .minZoom(17)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> traversalPermissions(VectorSourceLayer edges) {
    var permissionStyles = Arrays.stream(streetModes)
      .map(streetTraversalPermission ->
        StyleBuilder.ofId("permission " + streetTraversalPermission)
          .vectorSourceLayer(edges)
          .group(PERMISSIONS_GROUP)
          .typeLine()
          .filterValueInProperty(
            "permission",
            streetTraversalPermission.name(),
            StreetTraversalPermission.ALL.name()
          )
          .lineCap("butt")
          .lineColorMatch("permission", permissionColors(), BLACK)
          .lineWidth(LINE_WIDTH)
          .lineOffset(LINE_OFFSET)
          .minZoom(LINE_DETAIL_ZOOM)
          .maxZoom(MAX_ZOOM)
          .intiallyHidden()
      )
      .toList();

    var textStyle = StyleBuilder.ofId("permission-text")
      .vectorSourceLayer(edges)
      .group(PERMISSIONS_GROUP)
      .typeSymbol()
      .lineText("permission")
      .textOffset(1)
      .edgeFilter(EDGES_TO_DISPLAY)
      .minZoom(17)
      .maxZoom(MAX_ZOOM)
      .intiallyHidden();

    return ListUtils.combine(permissionStyles, List.of(textStyle));
  }

  private static List<StyleBuilder> noThruTraffic(VectorSourceLayer edges) {
    var noThruTrafficStyles = Arrays.stream(streetModes)
      .map(streetTraversalPermission ->
        StyleBuilder.ofId("no-thru-traffic " + streetTraversalPermission)
          .vectorSourceLayer(edges)
          .group(NO_THRU_TRAFFIC_GROUP)
          .typeLine()
          .filterValueInProperty(
            "noThruTraffic",
            streetTraversalPermission.name(),
            StreetTraversalPermission.ALL.name()
          )
          .lineCap("butt")
          .lineColorMatch("noThruTraffic", permissionColors(), BLACK)
          .lineWidth(LINE_WIDTH)
          .lineOffset(LINE_OFFSET)
          .minZoom(LINE_DETAIL_ZOOM)
          .maxZoom(MAX_ZOOM)
          .intiallyHidden()
      )
      .toList();

    var textStyle = StyleBuilder.ofId("no-thru-traffic-text")
      .vectorSourceLayer(edges)
      .group(NO_THRU_TRAFFIC_GROUP)
      .typeSymbol()
      .lineText("noThruTraffic")
      .textOffset(1)
      .edgeFilter(EDGES_TO_DISPLAY)
      .minZoom(17)
      .maxZoom(MAX_ZOOM)
      .intiallyHidden();

    return ListUtils.combine(noThruTrafficStyles, List.of(textStyle));
  }

  private static List<String> permissionColors() {
    return Arrays.stream(StreetTraversalPermission.values())
      .flatMap(p -> Stream.of(streetPermissionAsString(p), permissionColor(p)))
      .toList();
  }

  private static List<StyleBuilder> wheelchair(VectorSourceLayer edges) {
    return List.of(
      StyleBuilder.ofId("wheelchair-accessible")
        .vectorSourceLayer(edges)
        .group(WHEELCHAIR_GROUP)
        .typeLine()
        .lineColor(DARK_GREEN)
        .booleanFilter("wheelchairAccessible", true)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("wheelchair-inaccessible")
        .vectorSourceLayer(edges)
        .group(WHEELCHAIR_GROUP)
        .typeLine()
        .lineColor(RED)
        .booleanFilter("wheelchairAccessible", false)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static String permissionColor(StreetTraversalPermission p) {
    return switch (p) {
      case NONE -> BLACK;
      case PEDESTRIAN -> "#2ba812";
      case BICYCLE, PEDESTRIAN_AND_BICYCLE -> "#10d3b6";
      case CAR -> "#f92e13";
      case BICYCLE_AND_CAR, PEDESTRIAN_AND_CAR -> "#e25f8f";
      case ALL -> "#adb2b0";
    };
  }
}

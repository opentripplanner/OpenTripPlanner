package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.apis.vectortiles.Color.BLACK;
import static org.opentripplanner.apis.vectortiles.Color.BRIGHT_GREEN;
import static org.opentripplanner.apis.vectortiles.Color.DARK_BLUE;
import static org.opentripplanner.apis.vectortiles.Color.DARK_GREEN;
import static org.opentripplanner.apis.vectortiles.Color.DARK_ORANGE;
import static org.opentripplanner.apis.vectortiles.Color.DARK_RED;
import static org.opentripplanner.apis.vectortiles.Color.LIGHT_BLUE;
import static org.opentripplanner.apis.vectortiles.Color.LIGHT_MAGENTA;
import static org.opentripplanner.apis.vectortiles.Color.LIGHT_RED;
import static org.opentripplanner.apis.vectortiles.Color.MAGENTA;
import static org.opentripplanner.apis.vectortiles.Color.ORANGE;
import static org.opentripplanner.apis.vectortiles.Color.PURPLE;
import static org.opentripplanner.apis.vectortiles.Color.RED;
import static org.opentripplanner.apis.vectortiles.Color.TEAL;
import static org.opentripplanner.apis.vectortiles.Color.TURQUOISE;
import static org.opentripplanner.apis.vectortiles.Group.BICYCLE_SAFETY;
import static org.opentripplanner.apis.vectortiles.Group.EDGES;
import static org.opentripplanner.apis.vectortiles.Group.ELEVATION;
import static org.opentripplanner.apis.vectortiles.Group.NO_THRU_TRAFFIC;
import static org.opentripplanner.apis.vectortiles.Group.PERMISSIONS;
import static org.opentripplanner.apis.vectortiles.Group.RENTAL;
import static org.opentripplanner.apis.vectortiles.Group.STOPS;
import static org.opentripplanner.apis.vectortiles.Group.TRANSFERS;
import static org.opentripplanner.apis.vectortiles.Group.VERTICAL_TRANSPORTATION;
import static org.opentripplanner.apis.vectortiles.Group.VERTICES;
import static org.opentripplanner.apis.vectortiles.Group.WALK_SAFETY;
import static org.opentripplanner.apis.vectortiles.Group.WHEELCHAIR;
import static org.opentripplanner.inspector.vector.edge.EdgePropertyMapper.streetPermissionAsString;
import static org.opentripplanner.inspector.vector.geofencing.GeofencingZonesPropertyMapper.GEOFENCING_ZONE_TYPE;
import static org.opentripplanner.inspector.vector.geofencing.GeofencingZonesPropertyMapper.GEOFENCING_ZONE_TYPE_BUSINESS_AREA;
import static org.opentripplanner.inspector.vector.geofencing.GeofencingZonesPropertyMapper.GEOFENCING_ZONE_TYPE_NO_DROP_OFF;
import static org.opentripplanner.inspector.vector.geofencing.GeofencingZonesPropertyMapper.GEOFENCING_ZONE_TYPE_NO_TRAVERSAL;

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
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.standalone.config.debuguiconfig.BackgroundTileLayer;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
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
import org.opentripplanner.street.model.vertex.BarrierPassThroughVertex;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.ElevatorHopVertex;
import org.opentripplanner.street.model.vertex.OsmElevatorVertex;
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

  private static final int MAX_ZOOM = 23;
  private static final ZoomDependentNumber LARGE_CIRCLE_LINE_WIDTH = new ZoomDependentNumber(
    List.of(new ZoomStop(11, 0.5f), new ZoomStop(MAX_ZOOM, 5))
  );
  private static final ZoomDependentNumber LARGE_CIRCLE_RADIUS = new ZoomDependentNumber(
    List.of(new ZoomStop(11, 0.5f), new ZoomStop(MAX_ZOOM, 10))
  );
  private static final ZoomDependentNumber MEDIUM_CIRCLE_RADIUS = new ZoomDependentNumber(
    List.of(new ZoomStop(13, 1.4f), new ZoomStop(MAX_ZOOM, 10))
  );
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
  private static final ZoomDependentNumber LINE_QUARTER_WIDTH = new ZoomDependentNumber(
    List.of(new ZoomStop(LINE_DETAIL_ZOOM, 0.01f), new ZoomStop(MAX_ZOOM, 3))
  );
  private static final ZoomDependentNumber CIRCLE_STROKE = new ZoomDependentNumber(
    List.of(new ZoomStop(15, 0.2f), new ZoomStop(MAX_ZOOM, 3))
  );
  private static final Class<Edge>[] EDGES_TO_DISPLAY = new Class[] {
    StreetEdge.class,
    EscalatorEdge.class,
    PathwayEdge.class,
    ElevatorHopEdge.class,
    ElevatorBoardEdge.class,
    ElevatorAlightEdge.class,
    TemporaryPartialStreetEdge.class,
    TemporaryFreeEdge.class,
  };

  private static final StreetTraversalPermission[] STREET_MODES = new StreetTraversalPermission[] {
    StreetTraversalPermission.PEDESTRIAN,
    StreetTraversalPermission.BICYCLE,
    StreetTraversalPermission.CAR,
  };

  static StyleSpec build(
    VectorSourceLayer regularStops,
    VectorSourceLayer areaStops,
    VectorSourceLayer groupStops,
    VectorSourceLayer edges,
    VectorSourceLayer vertices,
    VectorSourceLayer geofencingZones,
    VectorSourceLayer rental,
    VectorSourceLayer transfers,
    List<BackgroundTileLayer> extraLayers
  ) {
    List<TileSource> vectorSources = Stream.of(
      regularStops,
      edges,
      vertices,
      geofencingZones,
      rental,
      transfers
    )
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
        transfers(transfers),
        rental(rental, geofencingZones),
        wheelchair(edges),
        noThruTraffic(edges),
        bicycleSafety(edges),
        walkSafety(edges),
        traversalPermissions(edges),
        edges(edges),
        elevation(edges, vertices),
        elevators(edges, vertices),
        vertices(vertices),
        stops(regularStops, areaStops, groupStops)
      )
    );
  }

  private static List<StyleBuilder> transfers(VectorSourceLayer transfers) {
    return List.of(
      StyleBuilder.ofId("flex-transfers")
        .group(TRANSFERS)
        .typeLine()
        .vectorSourceLayer(transfers)
        .lineColor(TEAL)
        .lineWidth(LINE_WIDTH)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
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
        .group(STOPS)
        .typeFill()
        .vectorSourceLayer(areaStops)
        .fillColor(BRIGHT_GREEN)
        .fillOpacity(0.5f)
        .fillOutlineColor(BLACK)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("group-stop")
        .group(STOPS)
        .typeFill()
        .vectorSourceLayer(groupStops)
        .fillColor(BRIGHT_GREEN)
        .fillOpacity(0.5f)
        .fillOutlineColor(BLACK)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("regular-stop")
        .group(STOPS)
        .typeCircle()
        .vectorSourceLayer(regularStops)
        .circleStroke(BLACK, LARGE_CIRCLE_LINE_WIDTH)
        .circleRadius(LARGE_CIRCLE_RADIUS)
        .circleColor("#fcf9fa")
        .minZoom(10)
        .maxZoom(MAX_ZOOM)
    );
  }

  private static List<StyleBuilder> vertices(VectorSourceLayer vertices) {
    return List.of(
      StyleBuilder.ofId("vertex")
        .group(VERTICES)
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
        .group(VERTICES)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .vertexFilter(VehicleParkingEntranceVertex.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(MEDIUM_CIRCLE_RADIUS)
        .circleColor(DARK_GREEN)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("barrier-vertex")
        .group(VERTICES)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .vertexFilter(BarrierVertex.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(MEDIUM_CIRCLE_RADIUS)
        .circleColor(DARK_RED)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("barrier-passthrough-vertex")
        .group(VERTICES)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .vertexFilter(BarrierPassThroughVertex.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(MEDIUM_CIRCLE_RADIUS)
        .circleColor(DARK_BLUE)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> elevators(VectorSourceLayer edges, VectorSourceLayer vertices) {
    return List.of(
      StyleBuilder.ofId("elevator-hop-edge")
        .group(VERTICAL_TRANSPORTATION)
        .typeLine()
        .vectorSourceLayer(edges)
        .edgeFilter(ElevatorHopEdge.class)
        .lineColor(ORANGE)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("elevator-board-edge")
        .group(VERTICAL_TRANSPORTATION)
        .typeLine()
        .vectorSourceLayer(edges)
        .edgeFilter(ElevatorBoardEdge.class)
        .lineColor(ORANGE)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("elevator-alight-edge")
        .group(VERTICAL_TRANSPORTATION)
        .typeLine()
        .vectorSourceLayer(edges)
        .edgeFilter(ElevatorAlightEdge.class)
        .lineColor(ORANGE)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("elevator-hop-vertex")
        .group(VERTICAL_TRANSPORTATION)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .vertexFilter(ElevatorHopVertex.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(
          new ZoomDependentNumber(List.of(new ZoomStop(15, 1), new ZoomStop(MAX_ZOOM, 7)))
        )
        .circleColor(ORANGE)
        .minZoom(15)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("osm-elevator-vertex")
        .group(VERTICAL_TRANSPORTATION)
        .typeCircle()
        .vectorSourceLayer(vertices)
        .vertexFilter(OsmElevatorVertex.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(
          new ZoomDependentNumber(List.of(new ZoomStop(15, 1), new ZoomStop(MAX_ZOOM, 7)))
        )
        .circleColor(ORANGE)
        .minZoom(15)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("escalator-edge")
        .group(VERTICAL_TRANSPORTATION)
        .typeLine()
        .vectorSourceLayer(edges)
        .edgeFilter(EscalatorEdge.class)
        .lineColor(ORANGE)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("stairs-edge")
        .group(VERTICAL_TRANSPORTATION)
        .typeLine()
        .vectorSourceLayer(edges)
        .booleanFilter("isStairs", true)
        .lineColor(ORANGE)
        .lineWidth(LINE_WIDTH)
        .lineOffset(LINE_OFFSET)
        .minZoom(6)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> rental(
    VectorSourceLayer rentalLayer,
    VectorSourceLayer geofencingZones
  ) {
    return List.of(
      StyleBuilder.ofId("rental-vehicle")
        .group(RENTAL)
        .typeCircle()
        .vectorSourceLayer(rentalLayer)
        .classFilter(VehicleRentalVehicle.class)
        .circleStroke(BLACK, CIRCLE_STROKE)
        .circleRadius(MEDIUM_CIRCLE_RADIUS)
        .circleColor(TEAL)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("rental-station")
        .group(RENTAL)
        .typeCircle()
        .vectorSourceLayer(rentalLayer)
        .classFilter(VehicleRentalStation.class)
        .circleStroke(BLACK, LARGE_CIRCLE_LINE_WIDTH)
        .circleRadius(LARGE_CIRCLE_RADIUS)
        .circleColor(TURQUOISE)
        .minZoom(13)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("geofencing-zones-no-drop-off")
        .group(RENTAL)
        .typeFill()
        .vectorSourceLayer(geofencingZones)
        .filterValueInProperty(GEOFENCING_ZONE_TYPE, GEOFENCING_ZONE_TYPE_NO_DROP_OFF)
        .fillColor(LIGHT_RED)
        .fillOpacity(0.3f)
        .fillOutlineColor(DARK_RED)
        .minZoom(10)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("geofencing-zones-no-traversal")
        .group(RENTAL)
        .typeFill()
        .vectorSourceLayer(geofencingZones)
        .filterValueInProperty(GEOFENCING_ZONE_TYPE, GEOFENCING_ZONE_TYPE_NO_TRAVERSAL)
        .fillColor(ORANGE)
        .fillOpacity(0.3f)
        .fillOutlineColor(DARK_ORANGE)
        .minZoom(10)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("geofencing-zones-business-area")
        .group(RENTAL)
        .typeFill()
        .vectorSourceLayer(geofencingZones)
        .filterValueInProperty(GEOFENCING_ZONE_TYPE, GEOFENCING_ZONE_TYPE_BUSINESS_AREA)
        .fillColor(LIGHT_BLUE)
        .fillOpacity(0.2f)
        .fillOutlineColor(DARK_BLUE)
        .minZoom(10)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden()
    );
  }

  private static List<StyleBuilder> edges(VectorSourceLayer edges) {
    return List.of(
      StyleBuilder.ofId("area-edge")
        .group(EDGES)
        .typeLine()
        .vectorSourceLayer(edges)
        .edgeFilter(AreaEdge.class)
        .lineColor(LIGHT_MAGENTA)
        .lineWidth(LINE_QUARTER_WIDTH)
        .lineOffset(LINE_OFFSET)
        .lineOpacity(0.5f)
        .minZoom(15)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("edge")
        .group(EDGES)
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
        .group(EDGES)
        .typeSymbol()
        .lineText("name")
        .vectorSourceLayer(edges)
        .edgeFilter(EDGES_TO_DISPLAY)
        .minZoom(17)
        .maxZoom(MAX_ZOOM)
        .intiallyHidden(),
      StyleBuilder.ofId("link")
        .group(EDGES)
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
        .group(ELEVATION)
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
        .group(ELEVATION)
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
        .group(BICYCLE_SAFETY)
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
        .group(BICYCLE_SAFETY)
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
        .group(WALK_SAFETY)
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
        .group(WALK_SAFETY)
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
    var permissionStyles = Arrays.stream(STREET_MODES)
      .map(streetTraversalPermission ->
        StyleBuilder.ofId("permission " + streetTraversalPermission)
          .vectorSourceLayer(edges)
          .group(PERMISSIONS)
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
      .group(PERMISSIONS)
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
    var noThruTrafficStyles = Arrays.stream(STREET_MODES)
      .map(streetTraversalPermission ->
        StyleBuilder.ofId("no-thru-traffic " + streetTraversalPermission)
          .vectorSourceLayer(edges)
          .group(NO_THRU_TRAFFIC)
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
      .group(NO_THRU_TRAFFIC)
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
        .group(WHEELCHAIR)
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
        .group(WHEELCHAIR)
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
      case NONE -> BLACK.hex();
      case PEDESTRIAN -> "#2ba812";
      case BICYCLE, PEDESTRIAN_AND_BICYCLE -> "#10d3b6";
      case CAR -> "#f92e13";
      case BICYCLE_AND_CAR, PEDESTRIAN_AND_CAR -> "#e25f8f";
      case ALL -> "#adb2b0";
    };
  }
}

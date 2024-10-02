package org.opentripplanner.apis.vectortiles;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.apis.vectortiles.model.StyleBuilder;
import org.opentripplanner.apis.vectortiles.model.StyleSpec;
import org.opentripplanner.apis.vectortiles.model.TileSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.RasterSource;
import org.opentripplanner.apis.vectortiles.model.VectorSourceLayer;
import org.opentripplanner.apis.vectortiles.model.ZoomDependentNumber;
import org.opentripplanner.apis.vectortiles.model.ZoomDependentNumber.ZoomStop;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;

/**
 *  A Mapbox/Mapblibre style specification for rendering debug information about transit and
 *  street data.
 */
public class DebugStyleSpec {

  private static final TileSource BACKGROUND_SOURCE = new RasterSource(
    "background",
    List.of("https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"),
    19,
    256,
    "© OpenStreetMap Contributors"
  );
  private static final String MAGENTA = "#f21d52";
  private static final String BRIGHT_GREEN = "#22DD9E";
  private static final String DARK_GREEN = "#136b04";
  private static final String PURPLE = "#BC55F2";
  private static final String BLACK = "#140d0e";

  private static final int MAX_ZOOM = 23;
  private static final ZoomDependentNumber LINE_WIDTH = new ZoomDependentNumber(
    1.3f,
    List.of(new ZoomStop(13, 0.5f), new ZoomStop(MAX_ZOOM, 10))
  );
  private static final ZoomDependentNumber FAT_LINE_WIDTH = new ZoomDependentNumber(
    1.3f,
    List.of(new ZoomStop(11, 1f), new ZoomStop(MAX_ZOOM, 12))
  );
  private static final ZoomDependentNumber CIRCLE_STROKE = new ZoomDependentNumber(
    1,
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
  private static final String STOPS = "Stops";
  private static final String VERTICES = "Vertices";
  private static final String TRAVERSAL_PERMISSIONS_GROU = "Traversal permissions";

  static StyleSpec build(
    VectorSourceLayer regularStops,
    VectorSourceLayer areaStops,
    VectorSourceLayer groupStops,
    VectorSourceLayer edges,
    VectorSourceLayer vertices
  ) {
    var vectorSources = Stream
      .of(regularStops, edges, vertices)
      .map(VectorSourceLayer::vectorSource);
    var allSources = Stream
      .concat(Stream.of(BACKGROUND_SOURCE), vectorSources)
      .collect(Collectors.toSet());
    return new StyleSpec(
      "OTP Debug Tiles",
      allSources,
      ListUtils.combine(
        List.of(StyleBuilder.ofId("background").typeRaster().source(BACKGROUND_SOURCE).minZoom(0)),
        traversalPermissions(edges),
        List.of(
          StyleBuilder
            .ofId("edge")
            .group(EDGES_GROUP)
            .typeLine()
            .vectorSourceLayer(edges)
            .lineColor(MAGENTA)
            .edgeFilter(EDGES_TO_DISPLAY)
            .lineWidth(LINE_WIDTH)
            .minZoom(6)
            .maxZoom(MAX_ZOOM)
            .intiallyHidden(),
          StyleBuilder
            .ofId("edge-name")
            .group(EDGES_GROUP)
            .typeSymbol()
            .lineText("name")
            .vectorSourceLayer(edges)
            .edgeFilter(EDGES_TO_DISPLAY)
            .minZoom(17)
            .maxZoom(MAX_ZOOM)
            .intiallyHidden(),
          StyleBuilder
            .ofId("link")
            .group(EDGES_GROUP)
            .typeLine()
            .vectorSourceLayer(edges)
            .lineColor(BRIGHT_GREEN)
            .edgeFilter(
              StreetTransitStopLink.class,
              StreetTransitEntranceLink.class,
              BoardingLocationToStopLink.class,
              StreetVehicleRentalLink.class,
              StreetVehicleParkingLink.class
            )
            .lineWidth(LINE_WIDTH)
            .minZoom(13)
            .maxZoom(MAX_ZOOM)
            .intiallyHidden(),
          StyleBuilder
            .ofId("vertex")
            .group(VERTICES)
            .typeCircle()
            .vectorSourceLayer(vertices)
            .circleStroke(BLACK, CIRCLE_STROKE)
            .circleRadius(
              new ZoomDependentNumber(1, List.of(new ZoomStop(15, 1), new ZoomStop(MAX_ZOOM, 7)))
            )
            .circleColor(PURPLE)
            .minZoom(15)
            .maxZoom(MAX_ZOOM)
            .intiallyHidden(),
          StyleBuilder
            .ofId("parking-vertex")
            .group(VERTICES)
            .typeCircle()
            .vectorSourceLayer(vertices)
            .vertexFilter(VehicleParkingEntranceVertex.class)
            .circleStroke(BLACK, CIRCLE_STROKE)
            .circleRadius(
              new ZoomDependentNumber(
                1,
                List.of(new ZoomStop(13, 1.4f), new ZoomStop(MAX_ZOOM, 10))
              )
            )
            .circleColor(DARK_GREEN)
            .minZoom(13)
            .maxZoom(MAX_ZOOM)
            .intiallyHidden(),
          StyleBuilder
            .ofId("area-stop")
            .group(STOPS)
            .typeFill()
            .vectorSourceLayer(areaStops)
            .fillColor(BRIGHT_GREEN)
            .fillOpacity(0.5f)
            .fillOutlineColor(BLACK)
            .minZoom(6)
            .maxZoom(MAX_ZOOM),
          StyleBuilder
            .ofId("group-stop")
            .group(STOPS)
            .typeFill()
            .vectorSourceLayer(groupStops)
            .fillColor(BRIGHT_GREEN)
            .fillOpacity(0.5f)
            .fillOutlineColor(BLACK)
            .minZoom(6)
            .maxZoom(MAX_ZOOM),
          StyleBuilder
            .ofId("regular-stop")
            .group(STOPS)
            .typeCircle()
            .vectorSourceLayer(regularStops)
            .circleStroke(
              BLACK,
              new ZoomDependentNumber(1, List.of(new ZoomStop(11, 0.5f), new ZoomStop(MAX_ZOOM, 5)))
            )
            .circleRadius(
              new ZoomDependentNumber(
                1,
                List.of(new ZoomStop(11, 0.5f), new ZoomStop(MAX_ZOOM, 10))
              )
            )
            .circleColor("#fcf9fa")
            .minZoom(10)
            .maxZoom(MAX_ZOOM)
        )
      )
    );
  }

  private static List<StyleBuilder> traversalPermissions(VectorSourceLayer edges) {
    var permissionStyles = Arrays
      .stream(StreetTraversalPermission.values())
      .map(p ->
        StyleBuilder
          .ofId(p.name())
          .group(TRAVERSAL_PERMISSIONS_GROU)
          .typeLine()
          .vectorSourceLayer(edges)
          .lineColor(permissionColor(p))
          .permissionsFilter(p)
          .lineWidth(FAT_LINE_WIDTH)
          .offsetFromProperty()
          .minZoom(6)
          .maxZoom(MAX_ZOOM)
          .intiallyHidden()
      )
      .toList();
    var textStyle = StyleBuilder
      .ofId("permission-text")
      .group(TRAVERSAL_PERMISSIONS_GROU)
      .typeSymbol()
      .lineText("permission")
      .vectorSourceLayer(edges)
      .edgeFilter(EDGES_TO_DISPLAY)
      .minZoom(17)
      .maxZoom(MAX_ZOOM);
    return ListUtils.combine(permissionStyles, List.of(textStyle));
  }

  private static String permissionColor(StreetTraversalPermission p) {
    return switch (p) {
      case NONE -> "#000";
      case PEDESTRIAN -> "#2ba812";
      case BICYCLE, PEDESTRIAN_AND_BICYCLE -> "#10d3b6";
      case CAR -> "#f92e13";
      case BICYCLE_AND_CAR, PEDESTRIAN_AND_CAR -> "#e25f8f";
      case ALL -> "#adb2b0";
    };
  }
}

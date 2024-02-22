package org.opentripplanner.apis.vectortiles;

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
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;

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
  private static final String GREEN = "#22DD9E";
  private static final String PURPLE = "#BC55F2";
  private static final String BLACK = "#140d0e";
  private static final int MAX_ZOOM = 23;
  private static final ZoomDependentNumber LINE_WIDTH = new ZoomDependentNumber(
    1.3f,
    List.of(new ZoomStop(13, 0.5f), new ZoomStop(MAX_ZOOM, 10))
  );
  private static final ZoomDependentNumber CIRCLE_STROKE = new ZoomDependentNumber(
    1,
    List.of(new ZoomStop(15, 0.2f), new ZoomStop(MAX_ZOOM, 3))
  );

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
      List.of(
        StyleBuilder.ofId("background").typeRaster().source(BACKGROUND_SOURCE).minZoom(0),
        StyleBuilder
          .ofId("edge")
          .typeLine()
          .vectorSourceLayer(edges)
          .lineColor(MAGENTA)
          .edgeFilter(
            StreetEdge.class,
            AreaEdge.class,
            EscalatorEdge.class,
            PathwayEdge.class,
            ElevatorHopEdge.class,
            TemporaryPartialStreetEdge.class,
            TemporaryFreeEdge.class
          )
          .lineWidth(LINE_WIDTH)
          .minZoom(13)
          .maxZoom(MAX_ZOOM)
          .intiallyHidden(),
        StyleBuilder
          .ofId("link")
          .typeLine()
          .vectorSourceLayer(edges)
          .lineColor(GREEN)
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
          .ofId("area-stop")
          .typeFill()
          .vectorSourceLayer(areaStops)
          .fillColor(GREEN)
          .fillOpacity(0.5f)
          .fillOutlineColor(BLACK)
          .minZoom(6)
          .maxZoom(MAX_ZOOM),
        StyleBuilder
          .ofId("group-stop")
          .typeFill()
          .vectorSourceLayer(groupStops)
          .fillColor(GREEN)
          .fillOpacity(0.5f)
          .fillOutlineColor(BLACK)
          .minZoom(6)
          .maxZoom(MAX_ZOOM),
        StyleBuilder
          .ofId("regular-stop")
          .typeCircle()
          .vectorSourceLayer(regularStops)
          .circleStroke(BLACK, 2)
          .circleRadius(
            new ZoomDependentNumber(1, List.of(new ZoomStop(11, 1), new ZoomStop(MAX_ZOOM, 10)))
          )
          .circleColor("#fcf9fa")
          .minZoom(10)
          .maxZoom(MAX_ZOOM)
      )
    );
  }
}

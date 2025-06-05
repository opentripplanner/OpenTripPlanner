package org.opentripplanner.graph_builder.module;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildService;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StationElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module takes advantage of the fact that in some cities, an authoritative linking location
 * for GTFS stops is provided by tags in the OSM data.
 * <p>
 * When OSM data is being loaded, certain entities that represent transit stops are made into
 * {@link OsmBoardingLocationVertex} instances. In some cities, these nodes have a ref=* tag which
 * gives the corresponding GTFS stop ID for the stop but the exact tag name is configurable. See
 * <a href="https://wiki.openstreetmap.org/wiki/Key:public_transport">the OSM wiki page</a>.
 * <p>
 * This module will attempt to link all transit stops and platforms to such nodes or way centroids
 * in the OSM data, based on the stop ID or stop code and ref tag. It is run before the main transit
 * stop linker, and if no linkage was created here, the main linker should create one based on
 * distance or other heuristics.
 */
public class OsmBoardingLocationsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(OsmBoardingLocationsModule.class);
  private static final LocalizedString LOCALIZED_PLATFORM_NAME = new LocalizedString(
    "name.platform"
  );
  private final double searchRadiusDegrees = SphericalDistanceLibrary.metersToDegrees(250);

  private final Graph graph;

  private final OsmInfoGraphBuildService osmInfoGraphBuildService;
  private final VertexFactory vertexFactory;

  private VertexLinker linker;

  @Inject
  public OsmBoardingLocationsModule(
    Graph graph,
    VertexLinker linker,
    OsmInfoGraphBuildService osmInfoGraphBuildService
  ) {
    this.graph = graph;
    this.osmInfoGraphBuildService = osmInfoGraphBuildService;
    this.vertexFactory = new VertexFactory(graph);
    this.linker = linker;
  }

  @Override
  public void buildGraph() {
    LOG.info("Improving boarding locations by checking OSM entities...");

    graph.index();
    int successes = 0;

    for (TransitStopVertex ts : graph.getVerticesOfType(TransitStopVertex.class)) {
      // if the street is already linked there is no need to linked it again,
      // could happened if using the prune isolated island
      boolean alreadyLinked = false;
      for (Edge e : ts.getOutgoing()) {
        if (e instanceof StreetTransitStopLink) {
          alreadyLinked = true;
          break;
        }
      }
      if (alreadyLinked) continue;
      // only connect transit stops that are not part of a pathway network
      if (!ts.hasPathways()) {
        if (!connectVertexToStop(ts, graph)) {
          LOG.debug("Could not connect {} at {}", ts.getStop().getCode(), ts.getCoordinate());
        } else {
          successes++;
        }
      }
    }
    LOG.info("Found {} OSM references which match a stop's id or code", successes);
  }

  private boolean connectVertexToStop(TransitStopVertex ts, Graph index) {
    if (connectVertexToNode(ts, index)) return true;

    if (connectVertexToWay(ts, index)) return true;

    return connectVertexToArea(ts, index);
  }

  private Envelope getEnvelope(TransitStopVertex ts) {
    Envelope envelope = new Envelope(ts.getCoordinate());

    double xscale = Math.cos((ts.getCoordinate().y * Math.PI) / 180);
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);
    return envelope;
  }

  /**
   * Connect a transit stop vertex into a boarding location area in the index.
   * <p>
   * A centroid vertex is generated and connected to the visibility vertices on the area edge.
   *
   * @return if the vertex has been connected
   */
  private boolean connectVertexToArea(TransitStopVertex ts, Graph graph) {
    RegularStop stop = ts.getStop();
    var nearbyAreaGroups = graph
      .findEdges(getEnvelope(ts))
      .stream()
      .filter(AreaEdge.class::isInstance)
      .map(AreaEdge.class::cast)
      .map(AreaEdge::getArea)
      .collect(Collectors.toSet());

    // Find a nearby area representing transit stop in OSM, linking to it if
    // stop code or id in ref= tag matches the GTFS stop code of this StopVertex.
    for (var areaGroup : nearbyAreaGroups) {
      for (Area area : areaGroup.getAreas()) {
        var platOpt = osmInfoGraphBuildService.findPlatform(area);
        if (platOpt.isPresent()) {
          var platform = platOpt.get();
          if (matchesReference(stop, platform.references())) {
            var boardingLocation = makeBoardingLocation(
              stop,
              platform.geometry().getCentroid(),
              platform.references(),
              area.getName()
            );
            linker.addPermanentAreaVertex(boardingLocation, areaGroup);
            linkBoardingLocationToStop(ts, stop.getCode(), boardingLocation);
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Connect a transit stop vertex to a boarding location way in the index.
   * <p>
   * The vertex is connected to the center of the way if one is found, splitting it if needed.
   *
   * @return if the vertex has been connected
   */
  private boolean connectVertexToWay(TransitStopVertex ts, Graph graph) {
    var stop = ts.getStop();
    var nearbyEdges = new HashMap<Platform, List<Edge>>();

    for (var edge : graph.findEdges(getEnvelope(ts))) {
      osmInfoGraphBuildService
        .findPlatform(edge)
        .ifPresent(platform -> {
          if (matchesReference(stop, platform.references())) {
            if (!nearbyEdges.containsKey(platform)) {
              var list = new ArrayList<Edge>();
              list.add(edge);
              nearbyEdges.put(platform, list);
            } else {
              nearbyEdges.get(platform).add(edge);
            }
          }
        });
    }

    for (var platformEdgeList : nearbyEdges.entrySet()) {
      Platform platform = platformEdgeList.getKey();
      var name = platform.name();
      var boardingLocation = makeBoardingLocation(
        stop,
        platform.geometry().getCentroid(),
        platform.references(),
        name
      );
      for (var vertex : linker.linkToSpecificStreetEdgesPermanently(
        boardingLocation,
        new TraverseModeSet(TraverseMode.WALK),
        LinkingDirection.BIDIRECTIONAL,
        platformEdgeList.getValue().stream().map(StreetEdge.class::cast).collect(Collectors.toSet())
      )) {
        linkBoardingLocationToStop(ts, stop.getCode(), vertex);
      }
      return true;
    }
    return false;
  }

  /**
   * Connect a transit stop vertex to a boarding location node.
   * <p>
   * The node is generated in the OSM processing step but we need to link it here.
   *
   * @return If the vertex has been connected.
   */
  private boolean connectVertexToNode(TransitStopVertex ts, Graph graph) {
    var nearbyBoardingLocations = graph
      .findVertices(getEnvelope(ts))
      .stream()
      .filter(OsmBoardingLocationVertex.class::isInstance)
      .map(OsmBoardingLocationVertex.class::cast)
      .collect(Collectors.toSet());

    for (var boardingLocation : nearbyBoardingLocations) {
      if (matchesReference(ts.getStop(), boardingLocation.references)) {
        if (!boardingLocation.isConnectedToStreetNetwork()) {
          linker.linkVertexPermanently(
            boardingLocation,
            new TraverseModeSet(TraverseMode.WALK),
            LinkingDirection.BIDIRECTIONAL,
            (osmBoardingLocationVertex, splitVertex) ->
              getConnectingEdges(boardingLocation, osmBoardingLocationVertex, splitVertex)
          );
        }
        linkBoardingLocationToStop(ts, ts.getStop().getCode(), boardingLocation);
        return true;
      }
    }
    return false;
  }

  private OsmBoardingLocationVertex makeBoardingLocation(
    RegularStop stop,
    Point centroid,
    Set<String> refs,
    I18NString name
  ) {
    var label = "platform-centroid/%s".formatted(stop.getId().toString());
    return vertexFactory.osmBoardingLocation(
      new Coordinate(centroid.getX(), centroid.getY()),
      label,
      refs,
      name
    );
  }

  private List<Edge> getConnectingEdges(
    OsmBoardingLocationVertex boardingLocation,
    Vertex osmBoardingLocationVertex,
    StreetVertex splitVertex
  ) {
    if (osmBoardingLocationVertex == splitVertex) {
      return List.of();
    }
    // the OSM boarding location vertex is not connected to the street network, so we
    // need to link it first
    return List.of(
      linkBoardingLocationToStreetNetwork(boardingLocation, splitVertex),
      linkBoardingLocationToStreetNetwork(splitVertex, boardingLocation)
    );
  }

  private StreetEdge linkBoardingLocationToStreetNetwork(StreetVertex from, StreetVertex to) {
    var line = GeometryUtils.makeLineString(List.of(from.getCoordinate(), to.getCoordinate()));
    return new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(line)
      .withName(LOCALIZED_PLATFORM_NAME)
      .withMeterLength(SphericalDistanceLibrary.length(line))
      .withPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE)
      .withBack(false)
      .buildAndConnect();
  }

  private void linkBoardingLocationToStop(
    TransitStopVertex ts,
    @Nullable String stopCode,
    StreetVertex boardingLocation
  ) {
    BoardingLocationToStopLink.createBoardingLocationToStopLink(ts, boardingLocation);
    BoardingLocationToStopLink.createBoardingLocationToStopLink(boardingLocation, ts);
    LOG.debug(
      "Connected {} ({}) to {} at {}",
      ts,
      stopCode,
      boardingLocation.getLabel(),
      boardingLocation.getCoordinate()
    );
  }

  private boolean matchesReference(StationElement<?, ?> stop, Collection<String> references) {
    var stopCode = stop.getCode();
    var stopId = stop.getId().getId();

    return (stopCode != null && references.contains(stopCode)) || references.contains(stopId);
  }
}

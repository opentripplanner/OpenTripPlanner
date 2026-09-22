package org.opentripplanner.graph_builder.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.StopFarFromBoardingLocationPlatform;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildService;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.LinkingDirection;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.streetadapter.VertexFactory;
import org.opentripplanner.transit.StopResolver;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.service.TransitRepository;
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
  private static final double SEARCH_RADIUS_DEGREES = SphericalDistanceLibrary.metersToDegrees(250);
  private static final double INSIDE_AREA_MARGIN_METERS = 0.2;
  /**
   * How far a stop may lie outside its OSM platform before the gap is reported as a data import
   * issue. A metre or so is normal between independently surveyed datasets; much more means one of
   * the two is misplaced.
   */
  private static final double FAR_FROM_PLATFORM_METERS = 5;
  /**
   * How far apart two linked vertices may be and still count as the same attachment point on a
   * platform way. Above floating-point noise between a way's forward and back edge, which project
   * to the same point, and below {@code VertexLinker}'s 0.1 m split-end tolerance, which keeps
   * genuinely distinct split points at least that far apart.
   */
  private static final double SAME_ATTACHMENT_POINT_TOLERANCE_METERS = 0.01;

  private final Graph graph;

  private final StopResolver stopResolver;
  private final OsmInfoGraphBuildService osmInfoGraphBuildService;
  private final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private final VertexFactory vertexFactory;
  private final VertexLinker linker;
  private final BoardingLocationCoordinateSource coordinateSource;
  private final DataImportIssueStore issueStore;

  private final Map<Platform, OsmBoardingLocationVertex> existingBoardingLocationsAtAreas;

  /**
   * @param transitRepository This module requires the timetable repository because at the time
   *                            of the instantiation the site repository is empty.
   */
  public OsmBoardingLocationsModule(
    Graph graph,
    TransitRepository transitRepository,
    VertexLinker linker,
    OsmInfoGraphBuildService osmInfoGraphBuildService,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    BoardingLocationCoordinateSource coordinateSource,
    DataImportIssueStore issueStore
  ) {
    this.graph = graph;
    this.stopResolver = id ->
      Objects.requireNonNull(transitRepository.getSiteRepository().getRegularStop(id));
    this.osmInfoGraphBuildService = osmInfoGraphBuildService;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.vertexFactory = new VertexFactory(graph);
    this.linker = linker;
    this.coordinateSource = coordinateSource;
    this.issueStore = issueStore;
    this.existingBoardingLocationsAtAreas = new HashMap<>();
  }

  @Override
  public void buildGraph() {
    LOG.info("Improving boarding locations by checking OSM entities...");

    graph.index();
    int successes = 0;

    for (TransitStopVertex ts : graph.getVerticesOfType(TransitStopVertex.class)) {
      // only connect transit stops that are not part of a pathway network
      if (!ts.hasPathways()) {
        var stop = stopResolver.getStop(ts.getId());
        if (!connectVertexToStop(ts, stop, graph)) {
          LOG.debug(
            "Could not connect {} ({}) at {}",
            ts.getId(),
            stop.getCode(),
            ts.getCoordinate()
          );
        } else {
          successes++;
        }
      }
    }
    LOG.info("Found {} OSM references which match a stop's id or code", successes);
  }

  private boolean connectVertexToStop(TransitStopVertex ts, RegularStop stop, Graph index) {
    if (connectVertexToNode(ts, stop, index)) {
      return true;
    }

    if (connectVertexToWay(ts, stop, index)) {
      return true;
    }

    return connectVertexToArea(ts, index);
  }

  private Envelope getEnvelope(TransitStopVertex ts) {
    Envelope envelope = new Envelope(ts.getCoordinate());

    double xscale = Math.cos((ts.getCoordinate().y * Math.PI) / 180);
    envelope.expandBy(SEARCH_RADIUS_DEGREES / xscale, SEARCH_RADIUS_DEGREES);
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
    var stop = stopResolver.getStop(ts.getId());
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
            var boardingLocation = makeBoardingLocationForPlatform(stop, platform, area.getName());
            // An area group with no visibility vertices cannot be linked into; another nearby
            // area may carry the same reference and be linkable.
            if (!linkIntoPlatformArea(boardingLocation, areaGroup, area, platform, stop)) {
              continue;
            }
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
  private boolean connectVertexToWay(TransitStopVertex ts, RegularStop stop, Graph graph) {
    var nearbyEdges = new HashMap<Platform, List<Edge>>();

    for (var edge : graph.findEdges(getEnvelope(ts))) {
      osmInfoGraphBuildService.findPlatform(edge).ifPresent(platform -> {
        if (matchesReference(stop, platform.references())) {
          nearbyEdges.computeIfAbsent(platform, _ -> new ArrayList<>()).add(edge);
        }
      });
    }

    return nearbyEdges
      .entrySet()
      .stream()
      .findFirst()
      .map(platformEdgeList -> {
        Platform platform = platformEdgeList.getKey();
        var boardingLocation = makeBoardingLocationForPlatform(stop, platform, platform.name());
        var linkedVertices = linker.linkToSpecificStreetEdgesPermanently(
          boardingLocation,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BIDIRECTIONAL,
          platformEdgeList
            .getValue()
            .stream()
            .map(StreetEdge.class::cast)
            .collect(Collectors.toSet())
        );
        var attachmentPoints = closestAttachmentPoint(boardingLocation, linkedVertices);
        for (var vertex : attachmentPoints) {
          reRegisterSplitEdgesWithPlatform(vertex, platform);
          linkBoardingLocationToStop(ts, stop.getCode(), vertex);
        }
        // The boarding location only carried the coordinate to link from - the stop is attached to
        // the split vertices instead - so it is left with no edges. Don't serialize it.
        graph.removeIfUnconnected(boardingLocation);
        // On failure the caller falls back to looking for a platform mapped as an area.
        return !attachmentPoints.isEmpty();
      })
      .orElse(false);
  }

  /**
   * Connect a transit stop vertex to a boarding location node.
   * <p>
   * The node is generated in the OSM processing step but we need to link it here.
   * <p>
   * {@link BoardingLocationCoordinateSource#TRANSIT} does not apply here: a tagged OSM node is
   * already a per-stop, provider-authoritative coordinate, so the centroid problem that option
   * addresses does not arise, and honouring it would mean relocating a vertex that OSM parsing has
   * already put in the graph and the spatial index.
   *
   * @return If the vertex has been connected.
   */
  private boolean connectVertexToNode(TransitStopVertex ts, RegularStop stop, Graph graph) {
    var nearbyBoardingLocations = graph
      .findVertices(getEnvelope(ts))
      .stream()
      .filter(OsmBoardingLocationVertex.class::isInstance)
      .map(OsmBoardingLocationVertex.class::cast)
      .collect(Collectors.toSet());

    for (var boardingLocation : nearbyBoardingLocations) {
      if (matchesReference(stop, boardingLocation.references)) {
        if (!boardingLocation.isConnectedToStreetNetwork()) {
          linker.linkVertexPermanently(
            boardingLocation,
            new TraverseModeSet(TraverseMode.WALK),
            LinkingDirection.BIDIRECTIONAL,
            (osmBoardingLocationVertex, splitVertex) ->
              getConnectingEdges(boardingLocation, osmBoardingLocationVertex, splitVertex)
          );
        }
        linkBoardingLocationToStop(ts, stop.getCode(), boardingLocation);
        return true;
      }
    }
    return false;
  }

  /**
   * Create (or, in {@code OSM} mode, reuse) the {@link OsmBoardingLocationVertex} used to link this
   * stop to the given platform. {@code OSM} mode places it at the platform centroid, shared between
   * all stops on the platform; {@code TRANSIT} mode gives each stop its own vertex at its own
   * coordinate, so stops on one platform stay distinct.
   */
  private OsmBoardingLocationVertex makeBoardingLocationForPlatform(
    RegularStop stop,
    Platform platform,
    I18NString name
  ) {
    if (coordinateSource == BoardingLocationCoordinateSource.TRANSIT) {
      return makeBoardingLocation(
        "platform-transit/%s".formatted(stop.getId().toString()),
        stop.getCoordinate().asJtsCoordinate(),
        platform.references(),
        name
      );
    }
    // OSM mode: share a single centroid vertex per platform between all stops referencing it.
    return existingBoardingLocationsAtAreas.computeIfAbsent(platform, _ ->
      makeBoardingLocation(
        "platform-centroid/%s".formatted(stop.getId().toString()),
        platform.geometry().getCentroid().getCoordinate(),
        platform.references(),
        name
      )
    );
  }

  /**
   * Wire a boarding location into the platform area it belongs to.
   * <p>
   * {@link VertexLinker} requires every visibility edge to stay inside the polygon, so a vertex even
   * a hair outside it gets at most the one or two edges that clear the check anyway - or a single
   * forced edge to the nearest visibility vertex, which routing detours through - and is not
   * registered as a visibility vertex, so the next stop on the platform cannot reach it directly.
   * <p>
   * A {@code TRANSIT} coordinate routinely is outside, the two datasets being surveyed
   * independently. Instead of moving the stop onto the platform, which would swallow a genuine
   * distance, an <em>access point</em> is placed just inside the polygon on its behalf: it takes the
   * visibility fan and the registration, joined to the boarding location by one edge of the true
   * length. In {@code OSM} mode the vertex is the platform's interior point and already inside, so
   * it is linked directly.
   *
   * @param area the matched platform sub-area, whose properties the edge to the access point carries
   * @return whether the boarding location was connected to the area
   */
  private boolean linkIntoPlatformArea(
    OsmBoardingLocationVertex boardingLocation,
    AreaGroup areaGroup,
    Area area,
    Platform platform,
    RegularStop stop
  ) {
    if (coordinateSource != BoardingLocationCoordinateSource.TRANSIT) {
      return linker.addPermanentAreaVertex(boardingLocation, areaGroup);
    }
    var insideCoordinate = ensureInsideArea(
      boardingLocation.getCoordinate(),
      areaGroup.getGeometry(),
      platform.geometry().getCoordinate()
    );
    if (insideCoordinate.equals2D(boardingLocation.getCoordinate())) {
      return linker.addPermanentAreaVertex(boardingLocation, areaGroup);
    }
    var accessPoint = vertexFactory.intersection(
      "platform-access/%s".formatted(stop.getId().toString()),
      insideCoordinate.x,
      insideCoordinate.y
    );
    if (!linker.addPermanentAreaVertex(accessPoint, areaGroup)) {
      return false;
    }
    var distanceToPlatform = SphericalDistanceLibrary.distance(
      boardingLocation.getCoordinate(),
      insideCoordinate
    );
    if (distanceToPlatform > FAR_FROM_PLATFORM_METERS) {
      issueStore.add(new StopFarFromBoardingLocationPlatform(stop, distanceToPlatform));
    }
    linkBoardingLocationToStreetNetwork(boardingLocation, accessPoint, area);
    linkBoardingLocationToStreetNetwork(accessPoint, boardingLocation, area);
    return true;
  }

  /**
   * The point just inside {@code areaGeometry} standing in for a {@code transitCoordinate} outside
   * the polygon, or {@code transitCoordinate} itself when already inside.
   * <p>
   * It is the foot of the perpendicular onto the polygon, stepped a margin further so it does not
   * sit on the boundary - the shortest way onto the platform. Aiming at the platform's interior
   * point instead would, on a long narrow platform, follow a line so oblique that it first crosses
   * the boundary tens of metres along the platform rather than a few metres onto it.
   *
   * @param interiorPoint the platform's own always-inside point, used only if the step somehow ends
   *                       up outside the polygon anyway.
   */
  private Coordinate ensureInsideArea(
    Coordinate transitCoordinate,
    Geometry areaGeometry,
    Coordinate interiorPoint
  ) {
    var geometryFactory = GeometryUtils.getGeometryFactory();
    var transitPoint = geometryFactory.createPoint(transitCoordinate);
    if (areaGeometry.contains(transitPoint)) {
      return transitCoordinate;
    }
    var onBoundary = DistanceOp.nearestPoints(areaGeometry, transitPoint)[0];
    var inside = stepBeyond(transitCoordinate, onBoundary, INSIDE_AREA_MARGIN_METERS);
    return areaGeometry.contains(geometryFactory.createPoint(inside)) ? inside : interiorPoint;
  }

  /** {@code marginMeters} past {@code to}, continuing along the line from {@code from}. */
  private static Coordinate stepBeyond(Coordinate from, Coordinate to, double marginMeters) {
    double totalMeters = SphericalDistanceLibrary.distance(from, to);
    if (totalMeters == 0) {
      return to;
    }
    double fraction = 1 + marginMeters / totalMeters;
    return new Coordinate(from.x + (to.x - from.x) * fraction, from.y + (to.y - from.y) * fraction);
  }

  private OsmBoardingLocationVertex makeBoardingLocation(
    String label,
    Coordinate coordinate,
    Set<String> refs,
    I18NString name
  ) {
    return vertexFactory.osmBoardingLocation(coordinate, label, refs, name);
  }

  /**
   * The platform association in {@link OsmInfoGraphBuildRepository} is keyed by edge reference and is
   * not carried over when linking splits a platform edge, so re-register the halves here; otherwise
   * a later stop on the same platform can no longer find it.
   * <p>
   * Only a genuine split produces a {@link SplitterVertex}. If the boarding location snapped to an
   * existing endpoint instead, the original edge is still registered and the endpoint's other
   * incident edges must not be tagged.
   */
  private void reRegisterSplitEdgesWithPlatform(StreetVertex vertex, Platform platform) {
    if (!(vertex instanceof SplitterVertex)) {
      return;
    }
    Stream.concat(vertex.getIncoming().stream(), vertex.getOutgoing().stream())
      .filter(StreetEdge.class::isInstance)
      .forEach(edge -> osmInfoGraphBuildRepository.addPlatform(edge, platform));
  }

  /**
   * A stop attaches to a platform way at one <em>point</em>, but at that point there is one vertex
   * per traversal direction: both the forward and the back edge are registered with the platform and
   * {@link VertexLinker} splits each separately, giving two co-located, mutually unconnected
   * vertices. The stop must be linked to both, or it is reachable from one end of the platform only.
   * <p>
   * {@link VertexLinker}'s duplicate-way heuristic can also return vertices at a <em>different</em>
   * point, where an earlier stop already split the way nearby. Those are within a fraction of a
   * millimetre of the correct ones in distance from the stop, so they are filtered out by position.
   *
   * @return every candidate co-located with the closest one: the attachment point the stop sits on,
   *          with all its traversal directions.
   */
  private static Set<StreetVertex> closestAttachmentPoint(
    OsmBoardingLocationVertex boardingLocation,
    Set<StreetVertex> candidates
  ) {
    var closest = candidates
      .stream()
      .min(
        Comparator.comparingDouble(v ->
          SphericalDistanceLibrary.distance(boardingLocation.getCoordinate(), v.getCoordinate())
        )
      );
    if (closest.isEmpty()) {
      return Set.of();
    }
    var attachmentPoint = closest.get().getCoordinate();
    return candidates
      .stream()
      .filter(
        v ->
          SphericalDistanceLibrary.distance(attachmentPoint, v.getCoordinate()) <=
          SAME_ATTACHMENT_POINT_TOLERANCE_METERS
      )
      .collect(Collectors.toSet());
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

  /**
   * A plain walkable-and-cyclable connector, for the node path: the boarding location is attached to
   * whatever street is nearest, which has no claim on the last few metres up to the node.
   */
  private StreetEdge linkBoardingLocationToStreetNetwork(StreetVertex from, StreetVertex to) {
    return linkBoardingLocationToStreetNetwork(from, to, null);
  }

  /**
   * @param area the platform the edge runs across, whose permission, safety factors and wheelchair
   *              accessibility it carries, matching the visibility edges {@link VertexLinker} builds
   *              in the same area. {@code null} for a plain walkable-and-cyclable link.
   */
  private StreetEdge linkBoardingLocationToStreetNetwork(
    StreetVertex from,
    StreetVertex to,
    @Nullable Area area
  ) {
    var line = GeometryUtils.makeLineString(List.of(from.getCoordinate(), to.getCoordinate()));
    var builder = new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(line)
      .withName(LOCALIZED_PLATFORM_NAME)
      .withMeterLength(GeometryUtils.sumDistances(line))
      .withBack(false);
    if (area == null) {
      builder.withPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    } else {
      builder
        .withPermission(area.getPermission())
        .withWalkSafetyFactor(area.getWalkSafety())
        .withBicycleSafetyFactor(area.getBicycleSafety())
        .withWheelchairAccessible(area.isWheelchairAccessible());
    }
    return builder.buildAndConnect();
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

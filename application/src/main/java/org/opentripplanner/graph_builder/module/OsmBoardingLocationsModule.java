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
   * How far a stop coordinate may lie outside the OSM platform it is linked to before the gap is
   * reported as a data import issue. A discrepancy of a metre or so is normal between independently
   * surveyed datasets; much more than that means one of the two is misplaced, or the platform
   * carries a reference it should not.
   */
  private static final double FAR_FROM_PLATFORM_METERS = 5;
  /**
   * How far apart two linked vertices may be and still count as the same attachment point on a
   * platform way. Comfortably above floating-point noise between the forward and the back edge of
   * the same way (which project to the same point), and comfortably below {@code VertexLinker}'s
   * 0.1 m split-end tolerance, which guarantees that two genuinely distinct split points on one way
   * are at least that far apart.
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
            // An area group with no visibility vertices cannot be linked into at all. Keep looking:
            // another nearby area may carry the same reference and be linkable.
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
          // Linking may have split a platform edge into two new edges. The platform association is
          // keyed by edge reference and is not carried over to the split halves, so re-register them
          // here; otherwise a later stop on the same platform can no longer match this platform.
          reRegisterSplitEdgesWithPlatform(vertex, platform);
          linkBoardingLocationToStop(ts, stop.getCode(), vertex);
        }
        // On this path the boarding location vertex only carries the coordinate to link from: the
        // stop is attached to the split vertices on the platform way, not to the vertex itself,
        // which is left with no edges. Drop it rather than serialize a vertex nothing can reach.
        graph.removeIfUnconnected(boardingLocation);
        // Report failure when linking found nothing to attach to, so the caller falls back to
        // looking for a platform mapped as an area instead of leaving the stop unlinked.
        return !attachmentPoints.isEmpty();
      })
      .orElse(false);
  }

  /**
   * Connect a transit stop vertex to a boarding location node.
   * <p>
   * The node is generated in the OSM processing step but we need to link it here.
   * <p>
   * {@link BoardingLocationCoordinateSource#TRANSIT} does not apply on this path, and the vertex
   * stays at the OSM node's coordinate. The centroid problem that option addresses does not arise
   * here - an OSM node tagged with a stop's reference is already a single, per-stop,
   * provider-authoritative coordinate, not a position shared between several stops. Honouring
   * {@code TRANSIT} would mean relocating a vertex that was created during OSM parsing and is
   * already in the graph and the spatial index, for which there is no clean operation. See
   * {@code connectVertexToWay} and {@code connectVertexToArea}, which create their vertices here
   * and so can place them freely.
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
   * stop to the given platform.
   * <p>
   * In {@code OSM} mode the vertex is placed at the platform centroid and shared between all stops
   * on the platform (so several stops collapse onto the same vertex). In {@code TRANSIT} mode each
   * stop gets its own vertex at its own coordinate from the transit data, so stops on the same
   * platform stay distinct.
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
   * {@link VertexLinker}'s area-visibility linking requires every candidate visibility edge to stay
   * entirely inside the polygon, so a vertex sitting even a hair outside it fails that check for
   * nearly every visibility vertex. It is then left with the one or two edges that happen to clear
   * the check anyway - or, when none do, with a single forced edge to whichever visibility vertex
   * happens to be nearest, which routing has to detour through. Either way it is also not registered
   * as a visibility vertex of the area, so the next stop on the same platform cannot connect to it
   * directly.
   * <p>
   * A {@code TRANSIT} coordinate routinely is outside: the stop coordinate and the OSM-mapped
   * platform boundary are surveyed independently. Rather than moving the stop onto the platform -
   * which would silently swallow a genuine distance - the vertex stays exactly where the transit
   * data puts it and an <em>access point</em> is placed just inside the polygon on its behalf. The
   * access point gets the visibility fan and the visibility-vertex registration; a single street
   * edge of its true length joins the two. A centimetre-scale discrepancy therefore costs a
   * centimetre-scale edge, and a stop genuinely off the platform gets a walk of the right length,
   * both without giving up the dense in-platform connectivity.
   * <p>
   * In {@code OSM} mode the vertex is the platform's own interior point and is inside the polygon by
   * construction, so it is linked directly.
   *
   * @param area the platform sub-area whose reference matched, whose properties the edge to the
   *              access point carries
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
   * The point just inside {@code areaGeometry} that stands in for {@code transitCoordinate} when
   * that coordinate is outside the polygon, or {@code transitCoordinate} itself when it is already
   * inside.
   * <p>
   * The returned point is found by walking from {@code transitCoordinate} towards the platform's own
   * (always-inside) {@code interiorPoint} up to the first boundary crossing, plus a small margin so
   * it is safely inside rather than sitting on the boundary itself.
   */
  private Coordinate ensureInsideArea(
    Coordinate transitCoordinate,
    Geometry areaGeometry,
    Coordinate interiorPoint
  ) {
    var geometryFactory = GeometryUtils.getGeometryFactory();
    if (areaGeometry.contains(geometryFactory.createPoint(transitCoordinate))) {
      return transitCoordinate;
    }
    var pathToInterior = geometryFactory.createLineString(new Coordinate[] {
      transitCoordinate,
      interiorPoint,
    });
    var crossings = areaGeometry.getBoundary().intersection(pathToInterior).getCoordinates();
    if (crossings.length == 0) {
      return interiorPoint;
    }
    var nearestCrossing = Stream.of(crossings)
      .min(Comparator.comparingDouble(c -> SphericalDistanceLibrary.distance(transitCoordinate, c)))
      .orElseThrow();
    return stepTowards(nearestCrossing, interiorPoint, INSIDE_AREA_MARGIN_METERS);
  }

  /**
   * A coordinate {@code marginMeters} further from {@code from} towards {@code to}, or {@code to}
   * itself if that is closer than {@code marginMeters}.
   */
  private static Coordinate stepTowards(Coordinate from, Coordinate to, double marginMeters) {
    double totalMeters = SphericalDistanceLibrary.distance(from, to);
    if (totalMeters <= marginMeters) {
      return to;
    }
    double fraction = marginMeters / totalMeters;
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
   * Linking a boarding location to a platform way splits the platform edge into two new edges. The
   * platform association in {@link OsmInfoGraphBuildRepository} is keyed by edge reference and is not
   * carried over to the split halves, so re-register the edges incident to the split vertex here.
   * Without this, a later stop on the same platform can no longer find (and link to) this platform.
   * <p>
   * Only a genuine split produces a {@link SplitterVertex}; if the boarding location snapped to an
   * existing endpoint of the platform edge, the original edge is untouched and still registered, so
   * there is nothing to do (and the endpoint's other incident edges must not be tagged).
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
   * A stop attaches to a platform way at a single <em>point</em>, but at that point there is one
   * vertex per traversal direction: OSM registers both the forward and the back edge of the way
   * with the platform, and {@link VertexLinker} splits each of them separately, producing two
   * co-located vertices that are not connected to each other. The stop must be linked to all of
   * them, or it becomes reachable from one end of the platform only.
   * <p>
   * {@link VertexLinker}'s "duplicate way" heuristic (meant for genuinely parallel edges, e.g. dual
   * carriageways) can additionally return vertices at a <em>different</em> point: if an earlier stop
   * on this same platform already split the way nearby, the two resulting halves are both
   * ~equidistant from this stop's coordinate and both get split again. Those extra vertices are
   * within a fraction of a millimetre of the correct ones in <em>distance from the stop</em>, but
   * they sit at a visibly different place on the platform, so they are filtered out by position
   * rather than by distance.
   *
   * @return every candidate co-located with the closest one, i.e. the attachment point the stop
   *          actually sits on, with all its traversal directions.
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
   * A connector edge for a boarding location that has no properties of its own to take, so it falls
   * back to a plain walkable-and-cyclable link. Used on the node path, where the boarding location
   * is an OSM node being attached to whatever street is nearest, and nothing says the street's
   * properties should apply to the last few metres up to the node.
   */
  private StreetEdge linkBoardingLocationToStreetNetwork(StreetVertex from, StreetVertex to) {
    return linkBoardingLocationToStreetNetwork(from, to, null);
  }

  /**
   * @param area the platform the edge runs across, whose permission, safety factors and wheelchair
   *              accessibility it then carries - matching the visibility edges {@link VertexLinker}
   *              builds within the same area. {@code null} to fall back to a plain walkable-and-
   *              cyclable link.
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

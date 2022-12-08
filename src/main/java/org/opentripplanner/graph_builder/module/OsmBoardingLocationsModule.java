package org.opentripplanner.graph_builder.module;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.NamedArea;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.service.TransitModel;
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
  private final double searchRadiusDegrees = SphericalDistanceLibrary.metersToDegrees(250);

  private final Graph graph;

  private final TransitModel transitModel;

  private VertexLinker linker;

  @Inject
  public OsmBoardingLocationsModule(Graph graph, TransitModel transitModel) {
    this.graph = graph;
    this.transitModel = transitModel;
  }

  @Override
  public void buildGraph() {
    LOG.info("Improving boarding locations by checking OSM entities...");

    StreetIndex streetIndex = graph.getStreetIndexSafe(transitModel.getStopModel());
    this.linker = streetIndex.getVertexLinker();
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
        if (!connectVertexToStop(ts, streetIndex, graph)) {
          LOG.debug("Could not connect {} at {}", ts.getStop().getCode(), ts.getCoordinate());
        } else {
          successes++;
        }
      }
    }
    LOG.info("Found {} OSM references which match a stop's id or code", successes);
  }

  @Override
  public void checkInputs() {
    //no inputs
  }

  private boolean connectVertexToStop(TransitStopVertex ts, StreetIndex index, Graph graph) {
    var stopCode = ts.getStop().getCode();
    var stopId = ts.getStop().getId().getId();
    Envelope envelope = new Envelope(ts.getCoordinate());

    double xscale = Math.cos(ts.getCoordinate().y * Math.PI / 180);
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);

    // if the boarding location is an OSM node it's generated in the OSM processing step but we need
    // link it here
    var nearbyBoardingLocations = index
      .getVerticesForEnvelope(envelope)
      .stream()
      .filter(OsmBoardingLocationVertex.class::isInstance)
      .map(OsmBoardingLocationVertex.class::cast)
      .collect(Collectors.toSet());

    for (var boardingLocation : nearbyBoardingLocations) {
      if (
        (stopCode != null && boardingLocation.references.contains(stopCode)) ||
        boardingLocation.references.contains(stopId)
      ) {
        if (!boardingLocation.isConnectedToStreetNetwork()) {
          linker.linkVertexPermanently(
            boardingLocation,
            new TraverseModeSet(TraverseMode.WALK),
            LinkingDirection.BOTH_WAYS,
            (osmBoardingLocationVertex, splitVertex) -> {
              // the OSM boarding location vertex is not connected to the street network, so we
              // need to link it first
              return List.of(
                linkBoardingLocationToStreetNetwork(boardingLocation, splitVertex),
                linkBoardingLocationToStreetNetwork(splitVertex, boardingLocation)
              );
            }
          );
        }
        linkBoardingLocationToStop(ts, stopCode, boardingLocation);
        return true;
      }
    }

    // if the boarding location is an OSM way (an area) then we are generating the vertex here and
    // use the AreaEdgeList to link it to the correct vertices of the platform edge
    var nearbyEdgeLists = index
      .getEdgesForEnvelope(envelope)
      .stream()
      .filter(AreaEdge.class::isInstance)
      .map(AreaEdge.class::cast)
      .map(AreaEdge::getArea)
      .collect(Collectors.toSet());

    // Iterate over all nearby areas representing transit stops in OSM, linking to them if they have a stop code or id
    // in their ref= tag that matches the GTFS stop code of this StopVertex.
    for (var edgeList : nearbyEdgeLists) {
      if (
        (stopCode != null && edgeList.references.contains(stopCode)) ||
        edgeList.references.contains(stopId)
      ) {
        var name = edgeList
          .getAreas()
          .stream()
          .findFirst()
          .map(NamedArea::getName)
          .orElse(new LocalizedString("name.platform"));
        var label = "platform-centroid/%s".formatted(ts.getStop().getId().toString());
        var centroid = edgeList.getGeometry().getCentroid();
        var boardingLocation = new OsmBoardingLocationVertex(
          graph,
          label,
          centroid.getX(),
          centroid.getY(),
          name,
          edgeList.references
        );
        edgeList.addVertex(boardingLocation);

        linkBoardingLocationToStop(ts, stopCode, boardingLocation);
        return true;
      }
    }
    return false;
  }

  private StreetEdge linkBoardingLocationToStreetNetwork(StreetVertex from, StreetVertex to) {
    var line = GeometryUtils.makeLineString(List.of(from.getCoordinate(), to.getCoordinate()));
    return new StreetEdge(
      from,
      to,
      line,
      new LocalizedString("name.platform"),
      SphericalDistanceLibrary.length(line),
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      false
    );
  }

  private void linkBoardingLocationToStop(
    TransitStopVertex ts,
    String stopCode,
    OsmBoardingLocationVertex boardingLocation
  ) {
    new BoardingLocationToStopLink(ts, boardingLocation);
    new BoardingLocationToStopLink(boardingLocation, ts);
    LOG.debug(
      "Connected {} ({}) to {} at {}",
      ts,
      stopCode,
      boardingLocation.getLabel(),
      boardingLocation.getCoordinate()
    );
  }
}

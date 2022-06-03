package org.opentripplanner.graph_builder.module;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.BoardingLocationToStopLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.OsmBoardingLocationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.LocalizedString;
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

  @Override
  public void buildGraph(
    Graph graph,
    HashMap<Class<?>, Object> extra,
    DataImportIssueStore issueStore
  ) {
    var streetIndex = graph.getStreetIndex();
    LOG.info("Improving boarding locations by checking OSM entities...");
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

  private boolean connectVertexToStop(TransitStopVertex ts, StreetVertexIndex index, Graph graph) {
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
          graph
            .getLinker()
            .linkVertexPermanently(
              boardingLocation,
              new TraverseModeSet(TraverseMode.WALK),
              LinkingDirection.BOTH_WAYS,
              (osmBoardingLocationVertex, splitVertex) -> {
                // the OSM boarding location vertex is not connected to the street network, so we
                // need to link it to the platform
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

    // if the boarding location is a OSM way (an area) then we are generating the vertex here and
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
        var name = edgeList.getAreas().get(0).getName();
        var label =
          "platform-centroid/%s".formatted(
              edgeList.visibilityVertices
                .stream()
                .map(IntersectionVertex::getLabel)
                .collect(Collectors.joining("/"))
            );
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

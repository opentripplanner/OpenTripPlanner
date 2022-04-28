package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.BoardingLocationToStopLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.OsmBoardingLocationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module takes advantage of the fact that in some cities, an authoritative linking location
 * for GTFS stops is provided by tags in the OSM data.
 * <p>
 * When OSM data is being loaded, certain entities that represent transit stops are made into
 * {@link OsmBoardingLocationVertex} instances. In some cities, these nodes have a ref=* tag which
 * gives the corresponding GFTS stop ID for the stop but the exact tag name is configurable. See
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
        if (!connectVertexToStop(ts, streetIndex, graph.getLinker())) {
          LOG.debug(
            "Could not connect " + ts.getStop().getCode() + " at " + ts.getCoordinate().toString()
          );
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

  private boolean connectVertexToStop(
    TransitStopVertex ts,
    StreetVertexIndex index,
    VertexLinker linker
  ) {
    var stopCode = ts.getStop().getCode();
    var stopId = ts.getStop().getId().getId();
    Envelope envelope = new Envelope(ts.getCoordinate());

    double xscale = Math.cos(ts.getCoordinate().y * Math.PI / 180);
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);
    Collection<Vertex> vertices = index.getVerticesForEnvelope(envelope);
    // Iterate over all nearby vertices representing transit stops in OSM, linking to them if they have a stop code
    // in their ref= tag that matches the GTFS stop code of this StopVertex.
    for (Vertex v : vertices) {
      if (!(v instanceof OsmBoardingLocationVertex osmVertex)) {
        continue;
      }

      if (
        (stopCode != null && osmVertex.references.contains(stopCode)) ||
        osmVertex.references.contains(stopId)
      ) {
        if (osmVertex.isConnectedToStreetNetwork()) {
          new StreetTransitStopLink(ts, osmVertex);
          new StreetTransitStopLink(osmVertex, ts);
        } else {
          linker.linkVertexPermanently(
            osmVertex,
            new TraverseModeSet(TraverseMode.WALK),
            LinkingDirection.BOTH_WAYS,
            (osmBoardingLocationVertex, splitVertex) -> {
              // the OSM boarding location vertex is not connected to the street network, so we
              // need to link it to the platform
              linkBoardingLocationToStreetNetwork(osmVertex, splitVertex);
              linkBoardingLocationToStreetNetwork(splitVertex, osmVertex);

              // and then link the TransitStopVertex to the BoardingLocationVertex
              return List.of(
                new BoardingLocationToStopLink(ts, osmVertex),
                new BoardingLocationToStopLink(osmVertex, ts)
              );
            }
          );
        }
        return true;
      }
    }
    return false;
  }

  private void linkBoardingLocationToStreetNetwork(StreetVertex from, StreetVertex to) {
    var line = GeometryUtils.makeLineString(List.of(from.getCoordinate(), to.getCoordinate()));
    new StreetEdge(
      from,
      to,
      line,
      from.getName(),
      SphericalDistanceLibrary.length(line),
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      false
    );
  }
}

package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import java.util.HashMap;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.OsmBoardingLocationVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module takes advantage of the fact that in some cities, an authoritative linking location
 * for GTFS stops is provided by tags in the OSM data.
 * <p>
 * When OSM data is being loaded, certain OSM nodes that represent transit stops are made into
 * {@link OsmBoardingLocationVertex} instances. In some cities, these nodes have a ref=* tag which
 * gives the corresponding GFTS stop ID for the stop. See
 * <a href="http://wiki.openstreetmap.org/wiki/Tag:highway%3Dbus_stop">the OSM wiki page</a>.
 * <p>
 * This module will attempt to link all transit stops to such nodes in the OSM data, based on the
 * stop ID or stop code and ref tag. It is run before the main transit stop linker, and if no
 * linkage was created here, the main linker should create one based on distance or other
 * heuristics.
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
    LOG.info("Improving boarding locations by checking OSM entities...");

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
        if (!connectVertexToStop(ts, graph.getStreetIndex())) {
          LOG.debug(
            "Could not connect " + ts.getStop().getCode() + " at " + ts.getCoordinate().toString()
          );
          // TODO OTP2 - Why is this commented out? Is it not a problem or is it to nosey?
          //LOG.warn(graph.addBuilderAnnotation(new StopUnlinked(ts)));
        }
      }
    }
  }

  @Override
  public void checkInputs() {
    //no inputs
  }

  private boolean connectVertexToStop(TransitStopVertex ts, StreetVertexIndex index) {
    String stopCode = ts.getStop().getCode();
    if (stopCode == null) {
      return false;
    }
    Envelope envelope = new Envelope(ts.getCoordinate());
    double xscale = Math.cos(ts.getCoordinate().y * Math.PI / 180);
    envelope.expandBy(searchRadiusDegrees / xscale, searchRadiusDegrees);
    Collection<Vertex> vertices = index.getVerticesForEnvelope(envelope);
    // Iterate over all nearby vertices representing transit stops in OSM, linking to them if they have a stop code
    // in their ref= tag that matches the GTFS stop code of this StopVertex.
    for (Vertex v : vertices) {
      if (!(v instanceof OsmBoardingLocationVertex tsv)) {
        continue;
      }

      // Only use stop codes for linking TODO: find better method to connect stops without stop code
      if (tsv.reference != null && tsv.reference.equals(stopCode)) {
        new StreetTransitStopLink(ts, tsv);
        new StreetTransitStopLink(tsv, ts);
        LOG.debug("Connected " + ts + " to " + tsv.getLabel());
        return true;
      }
    }
    return false;
  }
}

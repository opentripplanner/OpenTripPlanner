package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This module takes advantage of the fact that in some cities, an authoritative linking location for GTFS stops is
 * provided by tags in the OSM data.
 *
 * When OSM data is being loaded, certain OSM nodes that represent transit stops are made into TransitStopStreetVertex
 * instances. In some cities, these nodes have a ref=* tag which gives the corresponding GFTS stop ID for the stop.
 * See http://wiki.openstreetmap.org/wiki/Tag:highway%3Dbus_stop
 *
 * This module will attempt to link all transit stops to such nodes in the OSM data, based on the stop ID and ref tag.
 * It is run before the main transit stop linker, and if no linkage was created here, the main linker should create
 * one based on distance or other heuristics.
 */
public class TransitToTaggedStopsModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(TransitToTaggedStopsModule.class);

    StreetVertexIndexServiceImpl index;
    private double searchRadiusM = 250;
    private double searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(searchRadiusM);

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to tagged bus stops...");

        index = new StreetVertexIndexServiceImpl(graph);

        // iterate over a copy of vertex list because it will be modified
        ArrayList<Vertex> vertices = new ArrayList<>();
        vertices.addAll(graph.getVertices());

        for (TransitStop ts : Iterables.filter(vertices, TransitStop.class)) {
            // if the street is already linked there is no need to linked it again,
            // could happened if using the prune isolated island
            boolean alreadyLinked = false;
            for(Edge e:ts.getOutgoing()){
                if(e instanceof StreetTransitLink) {
                    alreadyLinked = true;
                    break;
                }
            }
            if(alreadyLinked) continue;
            // only connect transit stops that (a) are entrances, or (b) have no associated
            // entrances
            if (ts.isEntrance() || !ts.hasEntrances()) {
                boolean wheelchairAccessible = ts.hasWheelchairEntrance();
                if (!connectVertexToStop(ts, wheelchairAccessible)) {
                    LOG.debug("Could not connect " + ts.getStopCode() + " at " + ts.getCoordinate().toString());
                    //LOG.warn(graph.addBuilderAnnotation(new StopUnlinked(ts)));
                }
            }
        }
    }

    private boolean connectVertexToStop(TransitStop ts, boolean wheelchairAccessible) {
        String stopCode = ts.getStopCode();
        if (stopCode == null){
            return false;
        }
        Envelope envelope = new Envelope(ts.getCoordinate());
        double xscale = Math.cos(ts.getCoordinate().y * Math.PI / 180);
        envelope.expandBy(searchRadiusLat / xscale, searchRadiusLat);
        Collection<Vertex> vertices = index.getVerticesForEnvelope(envelope);
        // Iterate over all nearby vertices representing transit stops in OSM, linking to them if they have a stop code
        // in their ref= tag that matches the GTFS stop code of this TransitStop.
        for (Vertex v : vertices){
            if (!(v instanceof TransitStopStreetVertex)){
                continue;
            }
            TransitStopStreetVertex tsv = (TransitStopStreetVertex) v;

            // Only use stop codes for linking TODO: find better method to connect stops without stop code
            if (tsv.stopCode != null && tsv.stopCode.equals(stopCode)) {
                new StreetTransitLink(ts, tsv, wheelchairAccessible);
                new StreetTransitLink(tsv, ts, wheelchairAccessible);
                LOG.debug("Connected " + ts.toString() + " to " + tsv.getLabel());
                return true;
            }
        }
        return false;
    }

    @Override
    public void checkInputs() {
        //no inputs
    }
}

package org.opentripplanner.graph_builder.impl;

import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TransitToTaggedStopsGraphBuilderImpl implements GraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(TransitToTaggedStopsGraphBuilderImpl.class);

    StreetVertexIndexServiceImpl index;

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
        index.setup();

        // iterate over a copy of vertex list because it will be modified
        ArrayList<Vertex> vertices = new ArrayList<>();
        vertices.addAll(graph.getVertices());

        for (TransitStop ts : IterableLibrary.filter(vertices, TransitStop.class)) {
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
                    LOG.info("Could not connect " + ts.toString());
                    //LOG.warn(graph.addBuilderAnnotation(new StopUnlinked(ts)));
                }
            }
        }
    }

    private boolean connectVertexToStop(TransitStop ts, boolean wheelchairAccessible) {
        String ref = ts.getStopCode();
        if (ref == null) {
            return false;
        }
        Envelope envelope = new Envelope(ts.getCoordinate());
        envelope.expandBy(0.002); // ~= 100-200 meters
        Collection<Vertex> vertices = index.getVerticesForEnvelope(envelope);
        for (Vertex v : vertices){
            if (!(v instanceof IntersectionVertex)){
                continue;
            }
            if (((IntersectionVertex) v).getStopCode() != null && ((IntersectionVertex) v).getStopCode().matches(ref)){
                new StreetTransitLink(ts, (StreetVertex) v, wheelchairAccessible);
                new StreetTransitLink((StreetVertex) v, ts, wheelchairAccessible);
                LOG.info("Connected " + ts.toString() + " to " + v.getLabel());
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

package org.opentripplanner.graph_builder.linking;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.index.SpatialIndex;
import org.apache.lucene.store.TrackingDirectoryWrapper;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Linking seems to work.
 *
 *
 * Created by mabu on 20.5.2016.
 */
public class OriginDestinationLinker extends SimpleStreetSplitter {
    private static final Logger LOG = LoggerFactory.getLogger(OriginDestinationLinker.class);
    /**
     * Construct a new SimpleStreetSplitter. Be aware that only one SimpleStreetSplitter should be
     * active on a graph at any given time.
     *
     * @param graph
     * @param hashGridSpatialIndex If not null this index is used instead of creating new one
     */
    public OriginDestinationLinker(Graph graph, HashGridSpatialIndex<Edge> hashGridSpatialIndex) {
        super(graph, hashGridSpatialIndex);
    }

    /**
     * Construct a new SimpleStreetSplitter. Be aware that only one SimpleStreetSplitter should be
     * active on a graph at any given time.
     *
     * It creates new HashGridSpatialIndex
     *
     * @param graph
     */
    public OriginDestinationLinker(Graph graph) {
        super(graph);
    }

    public Vertex getClosestVertex(GenericLocation location, RoutingRequest options,
        boolean endVertex) {
        if (endVertex) {
            LOG.debug("Finding end vertex for {}", location);
        } else {
            LOG.debug("Finding start vertex for {}", location);
        }
        Coordinate coord = location.getCoordinate();
        //TODO: add nice name
        String name;
        if (endVertex) {
            name = "Destination ";
        } else {
            name = "Origin ";
        }
        TemporaryStreetLocation closest = new TemporaryStreetLocation(
            name + Math.random(), coord, new NonLocalizedString(name + Math.random()), endVertex);

        TraverseMode nonTransitMode = TraverseMode.WALK;
        //It can be null in tests
        if (options != null) {
            TraverseModeSet modes = options.modes;
            if (modes.getCar())
                nonTransitMode = TraverseMode.CAR;
            else if (modes.getWalk())
                nonTransitMode = TraverseMode.WALK;
            else if (modes.getBicycle())
                nonTransitMode = TraverseMode.BICYCLE;
        }

        if(!link(closest, nonTransitMode)) {
            LOG.warn("Couldn't link {}", location);
        }
        return closest;

    }

    /**
     * Make the appropriate type of link edges from a vertex
     *
     * @param from
     * @param to
     */
    @Override
    protected void makeLinkEdges(Vertex from, StreetVertex to) {
        TemporaryStreetLocation tse = (TemporaryStreetLocation) from;
        if (to instanceof TemporarySplitterVertex) {
            tse.setWheelchairAccessible(((TemporarySplitterVertex) to).isWheelchairAccessible());
        }
        if (tse.isEndVertex()) {
            LOG.debug("Linking end vertex to {} -> {}", to, tse);
            new TemporaryFreeEdge(to, tse);
        } else {
            LOG.debug("Linking start vertex to {} -> {}", tse, to);
            new TemporaryFreeEdge(tse, to);
        }
    }

    @Override
    protected void removeOriginalEdge(StreetEdge edge) {
        //Intentionally empty since we are creating temporary edges which should not change the graph
        //and they are removed anyway when thread is disposed
    }

    @Override
    protected void updateIndex(P2<StreetEdge> edges) {
        //Intentionally empty since we are creating temporary edges which should not change the graph
        //and they are removed anyway when thread is disposed
    }
}

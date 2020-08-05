package org.opentripplanner.graph_builder.linking;

import com.google.common.collect.Iterables;
import org.opentripplanner.routing.edgetype.StreetBikeParkLink;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TemporaryFreeEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes temporary and permanent edges between new vertices and ones that are already in graph
 */
public class EdgesMaker {

    private static final Logger LOG = LoggerFactory.getLogger(EdgesMaker.class);

    /**
     * Make temporary edges to origin/destination vertex in origin/destination search
     **/
    public void makeTemporaryEdges(TemporaryStreetLocation temporary, Vertex graphVertex) {
        propagateWheelchairAccessibleIfNeeded(temporary, graphVertex);
        if (temporary.isEndVertex()) {
            LOG.debug("Linking end vertex to {} -> {}", graphVertex, temporary);
            new TemporaryFreeEdge(graphVertex, temporary);
        } else {
            LOG.debug("Linking start vertex to {} -> {}", temporary, graphVertex);
            new TemporaryFreeEdge(temporary, graphVertex);
        }
    }

    private void propagateWheelchairAccessibleIfNeeded(TemporaryStreetLocation temporary, Vertex graphVertex) {
        if (graphVertex instanceof TemporarySplitterVertex) {
            temporary.setWheelchairAccessible(((TemporarySplitterVertex) graphVertex).isWheelchairAccessible());
        }
    }

    /**
     * Make permanent edges between two vertexes
     */
    public void makePermanentEdges(Vertex v1, StreetVertex v2) {
        if (v1 instanceof TransitStop) {
            makeTransitLinkEdges((TransitStop) v1, v2);
        } else if (v1 instanceof BikeRentalStationVertex) {
            makeBikeRentalLinkEdges((BikeRentalStationVertex) v1, v2);
        } else if (v1 instanceof BikeParkVertex) {
            makeBikeParkEdges((BikeParkVertex) v1, v2);
        } else {
            LOG.warn("Not supported type of vertex: {}", v1.getClass());
        }
    }

    /**
     * Make street transit link edges, unless they already exist.
     */
    private void makeTransitLinkEdges(TransitStop tstop, StreetVertex v) {
        // ensure that the requisite edges do not already exist
        // this can happen if we link to duplicate ways that have the same start/end vertices.
        for (StreetTransitLink e : Iterables.filter(tstop.getOutgoing(), StreetTransitLink.class)) {
            if (e.getToVertex() == v)
                return;
        }

        new StreetTransitLink(tstop, v, tstop.hasWheelchairEntrance());
        new StreetTransitLink(v, tstop, tstop.hasWheelchairEntrance());
    }

    /**
     * Make link edges for bike rental
     */
    private void makeBikeRentalLinkEdges(BikeRentalStationVertex from, StreetVertex to) {
        for (StreetBikeRentalLink sbrl : Iterables.filter(from.getOutgoing(), StreetBikeRentalLink.class)) {
            if (sbrl.getToVertex() == to)
                return;
        }

        new StreetBikeRentalLink(from, to);
        new StreetBikeRentalLink(to, from);
    }

    /**
     * Make bike park edges
     */
    private void makeBikeParkEdges(BikeParkVertex from, StreetVertex to) {
        for (StreetBikeParkLink sbpl : Iterables.filter(from.getOutgoing(), StreetBikeParkLink.class)) {
            if (sbpl.getToVertex() == to)
                return;
        }

        new StreetBikeParkLink(from, to);
        new StreetBikeParkLink(to, from);
    }
}

package org.opentripplanner.routing.graph;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.opentripplanner.common.geometry.Pointlike;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.edgetype.DirectEdge;
import org.opentripplanner.routing.graph.AbstractVertex.ValidEdgeTypes;

import com.vividsolutions.jts.geom.Coordinate;

public interface Vertex extends Serializable, Cloneable, Pointlike {

    public abstract String toString();
    
    /* EDGE LISTS */
    
    public abstract Collection<Edge> getOutgoing();
    public abstract void addOutgoing(Edge ee);
    public abstract void removeOutgoing(Edge ee);
    public abstract int getDegreeOut();

    public abstract Collection<Edge> getIncoming();
    public abstract void addIncoming(Edge ee);
    public abstract void removeIncoming(Edge ee);
    public abstract int getDegreeIn();

    /** Get an original, augmented, or replaced edgelist according to the supplied OverlayGraphs */
    public abstract Collection<Edge> getEdges(OverlayGraph extraEdges,
            OverlayGraph replacementEdges, boolean incoming);

    
    /* ACCESSOR METHODS */
    
    /** Get the distance from this vertex to the closest transit stop in meters. */
    public abstract double getDistanceToNearestTransitStop();
    // TODO: this is a candidate for no-arg message-passing style
    public abstract void setDistanceToNearestTransitStop(double distance);

    /** Get the longitude of the vertex */
    public abstract double getX();

    /** Get the latitude of the vertex */
    public abstract double getY();

    /** Every vertex has a label which is globally unique. */
    public abstract String getLabel();

    /** If this vertex is located on only one street, get that street's name. (RIGHT?) */
    public abstract String getName();

    public void setStreetName(String streetName);

    public abstract Coordinate getCoordinate();

    /** Get this vertex's unique index, that can serve as a hashcode or an index into a table */
    public abstract int getIndex();

    public abstract void setIndex(int index);

    public abstract void setGroupIndex(int groupIndex);

    public abstract int getGroupIndex();

    public abstract List<DirectEdge> getOutgoingStreetEdges();

    /**
     * Merge another vertex into this one.  Useful during graph construction for handling 
     * sequential non-branching streets, and empty dwells.
     */
    public abstract void mergeFrom(Graph graph, Vertex other);

    /**
     * Clear this vertex's outgoing and incoming edge lists, and remove all the edges
     * they contained from this vertex's neighbors.
     */
    public abstract void removeAllEdges();

    /** Trim edge lists */
    public abstract void compact();


    /* GRAPH COHERENCY AND TYPE CHECKING */

    public abstract ValidEdgeTypes getValidOutgoingEdgeTypes();

    public abstract ValidEdgeTypes getValidIncomingEdgeTypes();

    /** Check that all of this Vertex's incoming and outgoing edges are of the proper types */
    public abstract boolean edgeTypesValid();

}
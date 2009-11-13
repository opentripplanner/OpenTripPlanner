package org.opentripplanner.routing.core;

import org.opentripplanner.routing.algorithm.NegativeWeightException;

import com.vividsolutions.jts.geom.Geometry;

public interface Edge {

    public Vertex getFromVertex();

    public Vertex getToVertex();

    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException;
        
    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException;

    public TransportationMode getMode();

    public String getName();

    public String getDirection();

    public Geometry getGeometry();

    public String getStart();

    public String getEnd();

    public double getDistance();
}
package org.opentripplanner.common.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Subgraph {

    private final Set<Vertex> streetVertexSet;
    private final Set<Vertex> stopsVertexSet;

    public Subgraph(){
        streetVertexSet = new HashSet<>();
        stopsVertexSet = new HashSet<>();
    }

    public void addVertex(Vertex vertex){
        if(vertex instanceof TransitStopVertex){
            stopsVertexSet.add(vertex);
        }else{
            streetVertexSet.add(vertex);
        }
    }

    public boolean contains(Vertex vertex){
        return (streetVertexSet.contains(vertex) || stopsVertexSet.contains(vertex));
    }

    public int streetSize(){
        return streetVertexSet.size();
    }

    public int stopSize(){
        return stopsVertexSet.size();
    }

    public Vertex getRepresentativeVertex(){
        //TODO this is not very smart but good enough at the moment
        return streetVertexSet.iterator().next();
    }

    public Iterator<Vertex> streetIterator() {
        return streetVertexSet.iterator();
    }

    public Iterator<Vertex> stopIterator() {
        return stopsVertexSet.iterator();
    }
}

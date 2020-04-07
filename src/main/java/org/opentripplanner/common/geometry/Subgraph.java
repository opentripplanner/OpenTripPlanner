package org.opentripplanner.common.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;


public class Subgraph {

    private Set<Vertex> streetVertexSet;
    private Set<Vertex> stopsVertexSet;
    private ArrayList<Coordinate> vertexCoords;
    private Geometry convexHullAsGeom = null;
    private boolean newVertexAdded = true;

    public Subgraph(){
        streetVertexSet = new HashSet<Vertex>();
        stopsVertexSet = new HashSet<Vertex>();
        vertexCoords = new ArrayList<Coordinate>();
    }

    public void addVertex(Vertex vertex){
        if(vertex instanceof TransitVertex){
            stopsVertexSet.add(vertex);
        }else{
            streetVertexSet.add(vertex);
        }
        newVertexAdded = true;
        vertexCoords.add(vertex.getCoordinate());
    }

    public boolean contains(Vertex vertex){
        return (streetVertexSet.contains(vertex) || stopsVertexSet.contains(vertex));
    }

    public boolean containsStreet(Vertex vertex){
        return streetVertexSet.contains(vertex);
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

    private static GeometryFactory gf = new GeometryFactory();
    public Geometry getConvexHull() {
        if (newVertexAdded) {
            MultiPoint mp = gf.createMultiPoint(vertexCoords.toArray(new Coordinate[0]));
            newVertexAdded = false;
            mp.convexHull();
        }
        return convexHullAsGeom;
    }
}

package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import lombok.Getter;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Ben
 * Date: 27/12/12
 * Time: 16:32
 * To change this template use File | Settings | File Templates.
 */
public class Subgraph {

    private Set<Vertex> streetVertexSet;
    private Set<Vertex> stopsVertexSet;
    private ConvexHull convexHull = null;
    private ArrayList<Coordinate> vertexCoord;
    private Geometry convexHullAsGeom = null;
    private boolean addNewVertex = true;

    public Subgraph(){
        streetVertexSet = new HashSet<Vertex>();
        stopsVertexSet = new HashSet<Vertex>();
        vertexCoord = new ArrayList<Coordinate>();
    }

    public void addVertex(Vertex vertex){
        if(vertex instanceof TransitVertex){
            stopsVertexSet.add(vertex);
        }else{
            streetVertexSet.add(vertex);
        }
        addNewVertex = true;
        vertexCoord.add(vertex.getCoordinate());
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

    public Geometry getConvexHull(){
        if(addNewVertex) {
            convexHull = new ConvexHull(vertexCoord.toArray(new Coordinate[vertexCoord.size()]), new GeometryFactory());
            convexHullAsGeom = convexHull.getConvexHull();
            addNewVertex = false;
        }
        return convexHullAsGeom;
    }
}

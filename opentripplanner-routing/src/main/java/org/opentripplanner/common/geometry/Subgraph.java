/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.common.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;


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

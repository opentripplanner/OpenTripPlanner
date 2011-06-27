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

package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.spt.GraphPath;

/**
 * The Traveling Salesman Problem
 * Used in searches with 'intermediates'
 */
public class TSPPathFinder {
	
	/**
	 * A TSPPath is an ordered list of endpoints with a total cost to visit those endpoints
	 * @author novalis
	 *
	 */
    static class TSPPath implements Cloneable {
        List<Vertex> vertices;
        double cost;
        TSPPath(Vertex v, double cost) {
            vertices = new ArrayList<Vertex>();
            vertices.add(v);
            this.cost = cost;
        }
        
        public TSPPath(TSPPath tspPath) {
        	vertices = new ArrayList<Vertex>(tspPath.vertices);
        	cost = tspPath.cost;
        }

        public void addVertex(Vertex v, double cost) {
        	this.vertices.add(v);
        	this.cost += cost;
        }
        
		public TSPPath clone() {
        	return new TSPPath(this);
        }
    }
    
    public TSPPathFinder() {}
    
    private static TSPPath findShortestPathInternal(Vertex toVertex, Vertex fromVertex,
            Map<Vertex, HashMap<Vertex, GraphPath>> paths, Collection<Vertex> intermediates, double costSoFar) {
        
        if (intermediates.size() == 0) {
        	//base case: simply the path from the fromVertex to the toVertex
            TSPPath path = new TSPPath(toVertex, (paths.get(fromVertex).get(toVertex)).getWeight());
            return path;
        }
        
        List<Vertex> reducedIntermediates = new ArrayList<Vertex> ();
        reducedIntermediates.addAll(intermediates);
        TSPPath shortest = null;
        //find all paths through the remaining intermediate vertices, considering this as the start 
        for (Vertex vertex : intermediates) {
            reducedIntermediates.remove(vertex);
            TSPPath path = findShortestPathInternal(toVertex, vertex, paths, reducedIntermediates, costSoFar + paths.get(fromVertex).get(vertex).getWeight());
            if (shortest == null || shortest.cost > path.cost) {
                shortest = path;
                path.vertices.add(0, vertex);
            }
            reducedIntermediates.add(vertex);
        }
        return shortest;
    }
    
    public static GraphPath findShortestPath(Vertex toVertex, Vertex fromVertex,
            Map<Vertex, HashMap<Vertex, GraphPath>> paths, HashSet<Vertex> vertices, long time, TraverseOptions options) {
   
        TSPPath shortestPath = findShortestPathInternal(toVertex, fromVertex, paths, vertices, 0);
        
        Vertex firstIntermediate = shortestPath.vertices.get(0);
        
        HashMap<Vertex, GraphPath> pathsFromFV = paths.get(fromVertex);
        //get the path from the end of the first subpath
		GraphPath newPath = new GraphPath(pathsFromFV.get(firstIntermediate).states.getLast(), false);
        Vertex lastVertex = firstIntermediate;
        for (Vertex v : shortestPath.vertices.subList(1, shortestPath.vertices.size())) {
               State lastState = newPath.states.getLast();
               GraphPath subPath = paths.get(lastVertex).get(v);
               //add a leg-switching state
               LegSwitchingEdge legSwitchingEdge = new LegSwitchingEdge(lastVertex, lastVertex);
               lastState = legSwitchingEdge.traverse(lastState);
               newPath.edges.add(legSwitchingEdge);
        	   newPath.states.add(lastState);
               //add the next subpath
               for (Edge e : subPath.edges) {
            	   lastState = e.traverse(lastState);
            	   newPath.edges.add(e);
            	   newPath.states.add(lastState);
               }
               lastVertex = v;
        }
        
        return newPath;
    }

}

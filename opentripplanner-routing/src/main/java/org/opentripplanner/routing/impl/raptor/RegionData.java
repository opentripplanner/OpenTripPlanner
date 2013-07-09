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

package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class RegionData implements Serializable {
    private static final long serialVersionUID = 4835563169492792969L;

    public double[][] minWalk;

    public int[][] minTime;

    public int[] regionForVertex;

    public HashSet<RaptorRoute>[][] routes; //routes ever used on a shortest path between regions

    public HashSet<RaptorStop>[][] stops; //stops ever used on a shortest path between regions

    //a list of vertices for each region
    public ArrayList<ArrayList<Vertex>> verticesForRegion;

    public RegionData(int[] regionsForVertex) {
        this.regionForVertex = regionsForVertex;
    }

    public List<Integer> getRegionsForVertex(Vertex vertex) {
        return getRegionsForVertex(vertex, new HashSet<Vertex>());
    }

    public List<Integer> getRegionsForVertex(Vertex vertex, HashSet<Vertex> seen) {
        if (seen.contains(vertex)) {
            return Collections.emptyList();
        }
        seen.add(vertex);
        int index = regionForVertex[vertex.getIndex()];
        if (index >= 0) {
            return Arrays.asList(index);
        }
        ArrayList<Integer> regions = new ArrayList<Integer>();
        for (Edge e : vertex.getOutgoing()) {
            Vertex tov = e.getToVertex();
            for (int region : getRegionsForVertex(tov, seen)) {
                if (regions.contains(region))
                    continue;
                regions.add(region);
            }
        }
        return regions;
    }

    public int getRegionForVertex(Vertex v) {
        return regionForVertex[v.getIndex()];
    }

}

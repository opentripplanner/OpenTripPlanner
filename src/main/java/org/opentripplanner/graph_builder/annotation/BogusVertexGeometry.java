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

package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Vertex;

public class BogusVertexGeometry extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Vertex %s has NaN location; this will cause all sorts of " +
    		"problems. This is probably caused by a bug in the graph builder, but could " +
    		"conceivably happen with extremely bad GTFS or OSM data.";
    
    final Vertex vertex;
    
    public BogusVertexGeometry(Vertex vertex){
    	this.vertex = vertex;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, vertex);
    }

}

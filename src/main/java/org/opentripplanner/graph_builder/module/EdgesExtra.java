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

package org.opentripplanner.graph_builder.module;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.routing.graph.Edge;

/**
 * This class is used to store some temporary extra information about edges during graph building,
 * which for memory-saving reasons we do not want to store (even as a single pointer) in the Edge
 * instance itself.
 * 
 * Used as an optional "extra" parameter value in the GraphBuilderModule::buildGraph(Graph,
 * Map<Class, Object> extra). This "extra" graph builder module parameter is meant to exchange extra
 * transient information between graph builders.
 */
public class EdgesExtra {

    private Map<Edge, Object> extras = new HashMap<Edge, Object>();

    public EdgesExtra() {
    }

    public <T> void addExtra(Edge edge, T extra) {
        /*
         * For now, we only store one object per edge, but we could support multiple objects if
         * needed, as a map keyed with object class or more simply as a list / set.
         */
        if (extras.containsKey(edge))
            throw new IllegalArgumentException("Only a single object per edge is allowed. Edge: "
                    + edge);
        extras.put(edge, extra);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtra(Edge edge) {
        return (T) extras.get(edge);
    }
}

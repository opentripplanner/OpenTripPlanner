/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.graph_builder.impl.osm;

import java.util.HashSet;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

/**
 * This namer names streets as osm:way:osmid. Same as label previously
 * It is used for testing purposes.
 *
 * @author mabu
 */
public class DebugNamer implements CustomNamer {
    
    private static final String osmid_format = "osm:way:%d";

    @Override
    public String name(OSMWithTags way, String defaultName) {
        return String.format(osmid_format, way.getId());
    }

    @Override
    public void nameWithEdge(OSMWithTags way, StreetEdge edge) {
        String name = name(way, "");
        edge.setName(name);
    }

    @Override
    public void postprocess(Graph graph) {
        /*for (StreetEdge streetEdge: streets) {
            
        }*/
    }
    
}

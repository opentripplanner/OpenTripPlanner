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

package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.graph_builder.module.osm.PortlandCustomNamer;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * For when CreativeNamePicker/WayPropertySet is just not powerful enough.
 * 
 * @author novalis
 * 
 */
public interface CustomNamer {

    public String name(OSMWithTags way, String defaultName);

    public void nameWithEdge(OSMWithTags way, StreetEdge edge);

    public void postprocess(Graph graph);

    public void configure(JsonNode config);

    public class CustomNamerFactory {

        /**
         * Create a custom namer if needed, return null if not found / by default.
         */
        public static CustomNamer fromConfig(JsonNode config) {
            String type = null;
            if (config == null) {
                /* Empty block, fallback to default */
                return null;
            } else if (config.isTextual()) {
                /* Simplest form: { osmNaming : "portland" } */
                type = config.asText();
            } else if (config.has("type")) {
                /* Custom namer with a type: { osmNaming : { type : "foobar", param1 : 42 } } */
                type = config.path("type").asText(null);
            }
            if (type == null) {
                return null;
            }

            CustomNamer retval;
            switch (type) {
            case "portland":
                retval = new PortlandCustomNamer();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown osmNaming type: '%s'",
                        type));
            }
            // Configure the namer
            retval.configure(config);
            return retval;
        }
    }
}

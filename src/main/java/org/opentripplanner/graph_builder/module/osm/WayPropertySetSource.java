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

package org.opentripplanner.graph_builder.module.osm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;

/**
 * Factory interface for providing a {@link WayPropertySet} that determine how OSM
 * streets can be traversed in various modes and named.
 *
 * @author bdferris, novalis, seime
 */
public interface WayPropertySetSource {

    public WayPropertySet getWayPropertySet();
    
    public class WayPropertySetSourceFactory {

        /**
         * Return default way properties if not configured or if 
         */
        public static WayPropertySetSource fromConfig(JsonNode config) {
            String type = null;
            if (config == null || config instanceof MissingNode) {
                /* Empty block, fallback to default */
                return new DefaultWayPropertySetSource();
            } else if (config.isTextual()) {
                /* Simplest form: { wayPropertySet : "norway" } */
                type = config.asText();
            }

            WayPropertySetSource retval;
            switch (type) {
            // Support "default" as well
            case "default":
                retval = new DefaultWayPropertySetSource();
                break;
            case "norway":
                retval = new NorwayWayPropertySetSource();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown osmWayPropertySet: '%s'",
                        type));
            }

            return retval;
        }
    }

}

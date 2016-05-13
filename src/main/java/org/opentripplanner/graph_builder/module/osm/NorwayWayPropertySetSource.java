/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (props, at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for Norwegian roads. 
 * The main difference compared to the default property set is that most of the highway=trunk roads also allows walking and biking, 
 * where as some does not. 
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 * 
 *   
 * @author seime
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class NorwayWayPropertySetSource extends DefaultWayPropertySetSource {

    @Override
    public WayPropertySet getWayPropertySet() {
        
        // Override only the default values
        WayPropertySet props = new WayPropertySet();
        
        // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
        setProperties(props, "highway=trunk_link", StreetTraversalPermission.ALL, 2.06,
                2.06);
        setProperties(props, "highway=trunk", StreetTraversalPermission.ALL, 7.47, 7.47);
        
        // Don't recommend walking in trunk road tunnels (although actually legal unless explicitly forbidden)
        setProperties(props, "highway=trunk;tunnel=yes", StreetTraversalPermission.CAR, 7.47, 7.47);

        /*
         * Automobile speeds in Norway. General speed limit is 80kph unless signs says otherwise
         * 
         */
        setCarSpeed(props, "highway=motorway", 25); // 90kph
        setCarSpeed(props, "highway=motorway_link", 15); // = 54kph
        setCarSpeed(props, "highway=trunk", 22.22f); // 80kph
        setCarSpeed(props, "highway=trunk_link", 15); // = 54kph
        setCarSpeed(props, "highway=primary", 22.22f); // 80kph
        setCarSpeed(props, "highway=primary_link", 15); // = 54kph

        // Read the rest from the default set
        super.populateProperties(props);
        
        return props;
    }

   
}

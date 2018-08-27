/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for Finnish roads. FinlandWayPropertySetSource is derived from NorwayPropertySetSource by seime
 
 * The main difference compared to the default property set is that most of the highway=trunk roads also allows walking and biking, 
 * where as some does not. 
 * http://wiki.openstreetmap.org/wiki/Tag:highway%3Dtrunk
 * http://wiki.openstreetmap.org/wiki/Highway:International_equivalence
 * 
 *   
 * @author juusokor
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class HSLWayPropertySetSource implements WayPropertySetSource {

	@Override
	public void populateProperties(WayPropertySet props) {
        // Remove Helsinki city center service tunnel network from graph
        props.setProperties("highway=service;tunnel=yes;access=destination", StreetTraversalPermission.NONE);
		
        // Read the rest from the default set
		new DefaultWayPropertySetSource().populateProperties(props);
	}
}

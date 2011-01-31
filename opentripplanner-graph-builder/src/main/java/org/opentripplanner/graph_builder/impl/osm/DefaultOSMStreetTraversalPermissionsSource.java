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

package org.opentripplanner.graph_builder.impl.osm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Factory interface for providing a default collection of {@link StreetTraversalPermission}
 * permissions that determine how OSM streets can be traversed in various modes.
 * 
 * @author bdferris
 * @see StreetTraversalPermissionsSource
 * @see OpenStreetMapGraphBuilderImpl#setDefaultAccessPermissionsSource(StreetTraversalPermissionsSource)
 */
public class DefaultOSMStreetTraversalPermissionsSource implements StreetTraversalPermissionsSource {

    @Override
    public Map<String, StreetTraversalPermission> getPermissions() {
        Map<String, StreetTraversalPermission> m = new LinkedHashMap<String, StreetTraversalPermission>();

        m.put("highway=motorway", StreetTraversalPermission.CAR);
        m.put("highway=motorway_link", StreetTraversalPermission.CAR);
        m.put("highway=trunk", StreetTraversalPermission.CAR);
        m.put("highway=trunk_link", StreetTraversalPermission.CAR);

        m.put("highway=primary", StreetTraversalPermission.ALL);
        m.put("highway=primary_link", StreetTraversalPermission.ALL);
        m.put("highway=secondary", StreetTraversalPermission.ALL);
        m.put("highway=secondary_link", StreetTraversalPermission.ALL);
        m.put("highway=tertiary", StreetTraversalPermission.ALL);
        m.put("highway=tertiary_link", StreetTraversalPermission.ALL);
        m.put("highway=unclassified", StreetTraversalPermission.ALL);
        m.put("highway=residential", StreetTraversalPermission.ALL);
        m.put("highway=living_street", StreetTraversalPermission.ALL);
        m.put("highway=road", StreetTraversalPermission.ALL);
        m.put("highway=service", StreetTraversalPermission.ALL);
        m.put("highway=byway", StreetTraversalPermission.ALL);
        m.put("highway=track", StreetTraversalPermission.ALL);
        m.put("highway=mini_roundabout", StreetTraversalPermission.ALL);
        m.put("highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        m.put("cycleway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        m.put("highway=pedestrian", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        m.put("highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        m.put("highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        m.put("highway=footway", StreetTraversalPermission.PEDESTRIAN);
        m.put("highway=steps", StreetTraversalPermission.PEDESTRIAN);

        m.put("railway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        m.put("highway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        m.put("highway=construction", StreetTraversalPermission.NONE);
        m.put("highway=raceway", StreetTraversalPermission.NONE);

        m.put("__default__", StreetTraversalPermission.ALL);

        return m;
    }
}

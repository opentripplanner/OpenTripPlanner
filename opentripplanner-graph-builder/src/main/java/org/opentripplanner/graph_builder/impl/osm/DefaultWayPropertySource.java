/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.osm;


import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Factory interface for providing a default collection of {@link WayProperties}
 * that determine how OSM streets can be traversed in various modes.
 * 
 * Also supports 
 * 
 * @author bdferris, novalis
 * @see WayPropertySetSource
 * @see OpenStreetMapGraphBuilderImpl#setDefaultAccessPermissionsSource(props, WayPropertySetSource)
 */
public class DefaultWayPropertySource implements WayPropertySetSource {

    @Override
    public WayPropertySet getWayPropertySet() {
        WayPropertySet props = new WayPropertySet();

        setProperties(props, "highway=motorway", StreetTraversalPermission.CAR, 1.0, 1.0);
        setProperties(props, "highway=motorway_link", StreetTraversalPermission.CAR, 1.0, 1.0);
        setProperties(props, "highway=trunk", StreetTraversalPermission.CAR, 1.0, 1.0);
        setProperties(props, "highway=trunk_link", StreetTraversalPermission.CAR, 1.0, 1.0);

        setProperties(props, "highway=motorway;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        setProperties(props, "highway=motorway_link;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        setProperties(props, "highway=trunk;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.0, 1.5);
        setProperties(props, "highway=trunk_link;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);

        setProperties(props, "highway=primary", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=primary_link", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=secondary", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=secondary_link", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=tertiary", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=tertiary_link", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=unclassified", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=residential", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=living_street", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=road", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=service", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=byway", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=track", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=mini_roundabout", StreetTraversalPermission.ALL, 1.0, 1.0);
        setProperties(props, "highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        setProperties(props, "cycleway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        setProperties(props, "highway=pedestrian", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        setProperties(props, "highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        setProperties(props, "highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);

        setProperties(props, "highway=footway", StreetTraversalPermission.PEDESTRIAN, 1.0, 1.0);
        setProperties(props, "highway=pedestrian", StreetTraversalPermission.PEDESTRIAN, 1.0, 1.0);
        setProperties(props, "highway=steps", StreetTraversalPermission.PEDESTRIAN, 1.0, 1.0);

        setProperties(props, "railway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        setProperties(props, "highway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);

        setProperties(props, "highway=construction", StreetTraversalPermission.NONE, 1.0, 1.0);
        setProperties(props, "highway=raceway", StreetTraversalPermission.NONE, 1.0, 1.0);

        props.defaultProperties.setPermission(StreetTraversalPermission.ALL);

        return props;
    }

    private void setProperties(WayPropertySet propset, String spec, StreetTraversalPermission permission, double safety, double safetyBack) {
    	WayProperties properties = new WayProperties();
    	properties.setPermission(permission);
    	properties.setSafetyFeatures(new P2<Double>(safety, safetyBack));
    	propset.addProperties(new OSMSpecifier(spec), properties);
    }
}

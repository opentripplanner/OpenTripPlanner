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

        setProperties(props, "highway=motorway", StreetTraversalPermission.CAR, 8.33, 8.33);
        setProperties(props, "highway=motorway_link", StreetTraversalPermission.CAR, 8.33, 8.33);
        setProperties(props, "highway=trunk", StreetTraversalPermission.CAR, 8.33, 8.33);
        setProperties(props, "highway=trunk_link", StreetTraversalPermission.CAR, 8.33, 8.33);

        setProperties(props, "highway=motorway;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        setProperties(props, "highway=motorway_link;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        setProperties(props, "highway=trunk;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        setProperties(props, "highway=trunk_link;cycleway=lane", StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);

        setProperties(props, "highway=primary", StreetTraversalPermission.ALL, 2.06, 2.06);
        setProperties(props, "highway=primary_link", StreetTraversalPermission.ALL, 2.06, 2.06);
        setProperties(props, "highway=secondary", StreetTraversalPermission.ALL, 1.18, 1.18);
        setProperties(props, "highway=secondary_link", StreetTraversalPermission.ALL, 1.18, 1.18);
        setProperties(props, "highway=tertiary", StreetTraversalPermission.ALL);
        setProperties(props, "highway=tertiary_link", StreetTraversalPermission.ALL);
        setProperties(props, "highway=unclassified", StreetTraversalPermission.ALL);
        setProperties(props, "highway=residential", StreetTraversalPermission.ALL);
        setProperties(props, "highway=living_street", StreetTraversalPermission.ALL, 0.7, 0.7);
        setProperties(props, "highway=road", StreetTraversalPermission.ALL);
        setProperties(props, "highway=service", StreetTraversalPermission.ALL, 0.87, 0.87);
        setProperties(props, "highway=byway", StreetTraversalPermission.ALL);
        setProperties(props, "highway=mini_roundabout", StreetTraversalPermission.ALL, 0.9, 0.9);
        setProperties(props, "highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        
        setProperties(props, "highway=*;cycleway=shared_lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 0.77);
        setProperties(props, "highway=*;cycleway=lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.87, 0.87);
        setProperties(props, "highway=*;cycleway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        setProperties(props, "highway=*;cycleway=opposite", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.10, 1.45);
        setProperties(props, "highway=*;cycleway=opposite_lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.87);
        setProperties(props, "highway=*;cycleway=opposite_track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.73);
        
        setProperties(props, "highway=*;cycleway:left=shared_lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.77);
        setProperties(props, "highway=*;cycleway:left=lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.87);
        setProperties(props, "highway=*;cycleway:left=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.73);
        
        setProperties(props, "highway=*;cycleway:right=shared_lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 1.0);
        setProperties(props, "highway=*;cycleway:right=lane", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.87, 1.0);
        setProperties(props, "highway=*;cycleway:right=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 1.0);
        
        
        setProperties(props, "highway=pedestrian", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        setProperties(props, "highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        setProperties(props, "highway=path;bicycle=designated", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        setProperties(props, "highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.18, 1.18);
        setProperties(props, "highway=track", StreetTraversalPermission.ALL, 1.18, 1.18);
        
        setProperties(props, "highway=footway", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "highway=footway;bicycle=yes", StreetTraversalPermission.PEDESTRIAN, 0.84, 0.84); 
        setProperties(props, "highway=pedestrian", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "highway=pedestrian;bicycle=yes", StreetTraversalPermission.PEDESTRIAN, 0.73, 0.73);
        setProperties(props, "highway=steps", StreetTraversalPermission.PEDESTRIAN);

        setProperties(props, "railway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        setProperties(props, "highway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        setProperties(props, "highway=construction", StreetTraversalPermission.NONE);
        setProperties(props, "highway=raceway", StreetTraversalPermission.NONE);

        /* surface mixins */
        
        /* sand and fine gravel are deadly for bikes */
        setProperties(props, "surface=fine_gravel", StreetTraversalPermission.ALL, 100.0, 100.0, true);
        setProperties(props, "surface=sand", StreetTraversalPermission.ALL, 100.0, 100.0, true);
        
        setProperties(props, "surface=unpaved", StreetTraversalPermission.ALL, 1.8, 1.8, true);
        setProperties(props, "surface=compacted", StreetTraversalPermission.ALL, 1.8, 1.8, true);
        
        setProperties(props, "surface=cobblestones", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=paving_stones", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=glass_paver", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=pebblestones", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        
        setProperties(props, "surface=ground", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=dirt", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=earth", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=grass", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=mud", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=wood", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=metal", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=artifical_turf", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        
        /* Portland-local mixins */
        
        setProperties(props, "rlis:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
        setProperties(props, "rlis:bicycle=caution_area", StreetTraversalPermission.ALL, 1.6, 1.6, true);
        setProperties(props, "CCGIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
        setProperties(props, "CCGIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.6, 1.6, true);
        
        //it is extremly unsafe to ride directly on the Scotsman
        setProperties(props, "surface=tartan", StreetTraversalPermission.ALL, 2.0, 2.0, true);
        

        props.defaultProperties.setPermission(StreetTraversalPermission.ALL);

        return props;
    }

    private void setProperties(WayPropertySet propset, String spec, StreetTraversalPermission permission) {
    	setProperties(propset, spec, permission, 1.0, 1.0);
    }
    
    private void setProperties(WayPropertySet propset, String spec, StreetTraversalPermission permission, double safety, double safetyBack) {
    	setProperties(propset, spec, permission, safety, safetyBack, false);
    }
    
    private void setProperties(WayPropertySet propset, String spec, StreetTraversalPermission permission, double safety, double safetyBack, boolean mixin) {
    	WayProperties properties = new WayProperties();
    	properties.setPermission(permission);
    	properties.setSafetyFeatures(new P2<Double>(safety, safetyBack));
    	propset.addProperties(new OSMSpecifier(spec), properties, mixin);
    }
}

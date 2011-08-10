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
 * Factory interface for providing a default collection of {@link WayProperties} that determine how
 * OSM streets can be traversed in various modes.
 * 
 * Also supports
 * 
 * @author bdferris, novalis
 * @see WayPropertySetSource
 * @see OpenStreetMapGraphBuilderImpl#setDefaultAccessPermissionsSource(props, WayPropertySetSource)
 */
public class DefaultWayPropertySetSource implements WayPropertySetSource {

    @Override
    public WayPropertySet getWayPropertySet() {
        WayPropertySet props = new WayPropertySet();

        setProperties(props, "highway=motorway", StreetTraversalPermission.CAR, 8.33, 8.33);
        setProperties(props, "highway=motorway_link", StreetTraversalPermission.CAR, 8.33, 8.33);
        setProperties(props, "highway=trunk", StreetTraversalPermission.CAR, 8.33, 8.33);
        setProperties(props, "highway=trunk_link", StreetTraversalPermission.CAR, 8.33, 8.33);

        setProperties(props, "highway=motorway;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        setProperties(props, "highway=motorway_link;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);

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
        setProperties(props, "highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                0.73, 0.73);

        setProperties(props, "highway=*;cycleway=shared_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 0.77);
        setProperties(props, "highway=*;cycleway=lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.87, 0.87);
        setProperties(props, "highway=*;cycleway=track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        setProperties(props, "highway=*;cycleway=opposite",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.4);
        setProperties(props, "highway=*;cycleway=opposite_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.87);
        setProperties(props, "highway=*;cycleway=opposite_track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.73);

        setProperties(props, "highway=*;cycleway:left=shared_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.77);
        setProperties(props, "highway=*;cycleway:left=lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.87);
        setProperties(props, "highway=*;cycleway:left=track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.73);

        setProperties(props, "highway=*;cycleway:right=shared_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 1.0);
        setProperties(props, "highway=*;cycleway:right=lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.87, 1.0);
        setProperties(props, "highway=*;cycleway:right=track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 1.0);

        setProperties(props, "highway=pedestrian", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        setProperties(props, "highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                0.73, 0.73);
        setProperties(props, "highway=path;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        setProperties(props, "highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                1.18, 1.18);
        setProperties(props, "highway=track", StreetTraversalPermission.ALL, 1.18, 1.18);

        setProperties(props, "highway=pedestrian", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "highway=pedestrian;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN, 0.73, 0.73);
        setProperties(props, "highway=steps", StreetTraversalPermission.PEDESTRIAN);

        setProperties(props, "railway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
        setProperties(props, "highway=platform", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        setProperties(props, "highway=construction", StreetTraversalPermission.NONE);
        setProperties(props, "highway=raceway", StreetTraversalPermission.NONE);

        setProperties(props, "highway=residential;cycleway=lane", StreetTraversalPermission.ALL,
                0.77, 0.77);
        setProperties(props, "highway=tertiary;cycleway=lane", StreetTraversalPermission.ALL, 0.87,
                0.87);
        setProperties(props, "highway=secondary;cycleway=lane", StreetTraversalPermission.ALL,
                0.93, 0.93);
        setProperties(props, "highway=primary;cycleway=lane", StreetTraversalPermission.ALL, 1.15,
                1.15);
        setProperties(props, "highway=primary_link;cycleway=lane", StreetTraversalPermission.ALL,
                1.15, 1.15);
        setProperties(props, "highway=trunk;cycleway=lane", StreetTraversalPermission.ALL, 1.5, 1.5);
        setProperties(props, "highway=trunk_link;cycleway=lane", StreetTraversalPermission.ALL,
                1.5, 1.5);

        setProperties(props, "highway=residential;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 0.9, 0.77);
        setProperties(props, "highway=tertiary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1, 0.87);
        setProperties(props, "highway=secondary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.18, 0.93);
        setProperties(props, "highway=primary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        setProperties(props, "highway=primary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        setProperties(props, "highway=trunk;cycleway=opposite_lane", StreetTraversalPermission.ALL,
                7.47, 1.5);
        setProperties(props, "highway=trunk_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 7.47, 1.5);

        setProperties(props, "highway=residential;cycleway=track", StreetTraversalPermission.ALL,
                0.7, 0.7);
        setProperties(props, "highway=tertiary;cycleway=track", StreetTraversalPermission.ALL,
                0.75, 0.75);
        setProperties(props, "highway=secondary;cycleway=track", StreetTraversalPermission.ALL,
                0.8, 0.8);
        setProperties(props, "highway=primary;cycleway=track", StreetTraversalPermission.ALL, 0.85,
                0.85);
        setProperties(props, "highway=primary_link;cycleway=track", StreetTraversalPermission.ALL,
                0.85, 0.85);
        setProperties(props, "highway=trunk;cycleway=track", StreetTraversalPermission.ALL, 0.9,
                0.9);
        setProperties(props, "highway=trunk_link;cycleway=track", StreetTraversalPermission.ALL,
                0.9, 0.9);

        setProperties(props, "highway=residential;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 0.9, 0.7);
        setProperties(props, "highway=tertiary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1, 0.75);
        setProperties(props, "highway=secondary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.18, 0.8);
        setProperties(props, "highway=primary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        setProperties(props, "highway=primary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        setProperties(props, "highway=trunk;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 7.47, 0.9);
        setProperties(props, "highway=trunk_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 7.47, 0.9);

        setProperties(props, "highway=residential;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        setProperties(props, "highway=tertiary;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.83, 0.83);
        setProperties(props, "highway=secondary;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.91, 0.91);

        setProperties(props, "highway=residential;cycleway=opposite",
                StreetTraversalPermission.ALL, 0.9, 1.3485);
        setProperties(props, "highway=tertiary;cycleway=opposite", StreetTraversalPermission.ALL,
                1, 1.45);
        setProperties(props, "highway=secondary;cycleway=opposite", StreetTraversalPermission.ALL,
                1.18, 1.711);
        setProperties(props, "highway=primary;cycleway=opposite", StreetTraversalPermission.ALL,
                2.06, 2.987);

        /* properties for cycleway:right */
        setProperties(props, "highway=residential;cycleway:right=lane",
                StreetTraversalPermission.ALL, 0.77, 1.0);
        setProperties(props, "highway=tertiary;cycleway:right=lane", StreetTraversalPermission.ALL,
                0.87, 1.0);
        setProperties(props, "highway=secondary;cycleway:right=lane",
                StreetTraversalPermission.ALL, 0.93, 1.18);
        setProperties(props, "highway=primary;cycleway:right=lane", StreetTraversalPermission.ALL,
                1.15, 2.06);
        setProperties(props, "highway=primary_link;cycleway:right=lane",
                StreetTraversalPermission.ALL, 1.15, 2.06);
        setProperties(props, "highway=trunk;cycleway:right=lane", StreetTraversalPermission.ALL,
                1.5, 8.33);
        setProperties(props, "highway=trunk_link;cycleway:right=lane",
                StreetTraversalPermission.ALL, 1.5, 8.33);

        setProperties(props, "highway=residential;cycleway:right=track",
                StreetTraversalPermission.ALL, 0.7, 1.0);
        setProperties(props, "highway=tertiary;cycleway:right=track",
                StreetTraversalPermission.ALL, 0.75, 1.0);
        setProperties(props, "highway=secondary;cycleway:right=track",
                StreetTraversalPermission.ALL, 0.8, 1.18);
        setProperties(props, "highway=primary;cycleway:right=track", StreetTraversalPermission.ALL,
                0.85, 2.06);
        setProperties(props, "highway=primary_link;cycleway:right=track",
                StreetTraversalPermission.ALL, 0.85, 2.06);
        setProperties(props, "highway=trunk;cycleway:right=track", StreetTraversalPermission.ALL,
                0.9, 8.33);
        setProperties(props, "highway=trunk_link;cycleway:right=track",
                StreetTraversalPermission.ALL, 0.9, 8.33);

        setProperties(props, "highway=residential;cycleway:right=shared_lane",
                StreetTraversalPermission.ALL, 0.77, 1.0);
        setProperties(props, "highway=tertiary;cycleway:right=shared_lane",
                StreetTraversalPermission.ALL, 0.83, 1.0);
        setProperties(props, "highway=secondary;cycleway:right=shared_lane",
                StreetTraversalPermission.ALL, 0.91, 1.18);

        /* properties for cycleway:left */
        setProperties(props, "highway=residential;cycleway:left=lane",
                StreetTraversalPermission.ALL, 1.0, 0.77);
        setProperties(props, "highway=tertiary;cycleway:left=lane", StreetTraversalPermission.ALL,
                1.0, 0.87);
        setProperties(props, "highway=secondary;cycleway:left=lane", StreetTraversalPermission.ALL,
                1.18, 0.93);
        setProperties(props, "highway=primary;cycleway:left=lane", StreetTraversalPermission.ALL,
                2.06, 1.15);
        setProperties(props, "highway=primary_link;cycleway:left=lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        setProperties(props, "highway=trunk;cycleway:left=lane", StreetTraversalPermission.ALL,
                8.33, 1.5);
        setProperties(props, "highway=trunk_link;cycleway:left=lane",
                StreetTraversalPermission.ALL, 8.33, 1.5);

        setProperties(props, "highway=residential;cycleway:left=track",
                StreetTraversalPermission.ALL, 1.0, 0.7);
        setProperties(props, "highway=tertiary;cycleway:left=track", StreetTraversalPermission.ALL,
                1.0, 0.75);
        setProperties(props, "highway=secondary;cycleway:left=track",
                StreetTraversalPermission.ALL, 1.18, 0.8);
        setProperties(props, "highway=primary;cycleway:left=track", StreetTraversalPermission.ALL,
                2.06, 0.85);
        setProperties(props, "highway=primary_link;cycleway:left=track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        setProperties(props, "highway=trunk;cycleway:left=track", StreetTraversalPermission.ALL,
                8.33, 0.9);
        setProperties(props, "highway=trunk_link;cycleway:left=track",
                StreetTraversalPermission.ALL, 8.33, 0.9);

        setProperties(props, "highway=residential;cycleway:left=shared_lane",
                StreetTraversalPermission.ALL, 1.0, 0.77);
        setProperties(props, "highway=tertiary;cycleway:left=shared_lane",
                StreetTraversalPermission.ALL, 1.0, 0.83);
        setProperties(props, "highway=secondary;cycleway:left=shared_lane",
                StreetTraversalPermission.ALL, 1.18, 0.91);


        setProperties(props, "footway=sidewalk;highway=footway", 
                StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "footway=sidewalk;highway=footway;bicycle=designated", 
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
        setProperties(props, "footway=sidewalk;highway=footway;RLIS:bicycle=designated", 
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.84, 0.84);
        setProperties(props, "highway=footway;bicycle=designated", 
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.73, 0.73);
        setProperties(props, "footway=sidewalk;highway=footway;bicycle=yes", 
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 2.5, 2.5);
        
        setProperties(props, "highway=footway", 
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 0.77);
        setProperties(props, "highway=footway;bicycle=yes", 
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 0.77);

        /* surface mixins */

        /* sand and fine gravel are deadly for bikes */
        setProperties(props, "surface=fine_gravel", StreetTraversalPermission.ALL, 100.0, 100.0,
                true);
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
        setProperties(props, "surface=artifical_turf", StreetTraversalPermission.ALL, 1.5, 1.5,
                true);
        
        /* Portland-local mixins */

        setProperties(props, "RLIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97,
                true);
        setProperties(props, "RLIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.6, 1.6,
                true);
        setProperties(props, "CCGIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97,
                true);
        setProperties(props, "CCGIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.6, 1.6,
                true);

        // it is extremly unsafe to ride directly on the Scotsman
        setProperties(props, "surface=tartan", StreetTraversalPermission.ALL, 3.0, 3.0, true);

        props.defaultProperties.setPermission(StreetTraversalPermission.ALL);

        /* and the notes */
        createNotes(props, "notes=*", "{notes}");
        createNotes(props, "rlis:bicycle=caution_area", "Caution!");
        createNotes(props, "CCGIS:bicycle=caution_area", "Caution!");
        createNotes(props, "surface=unpaved", "Unpaved surface");
        createNotes(props, "surface=compacted", "Unpaved surface");
        createNotes(props, "surface=ground", "Unpaved surface");
        createNotes(props, "surface=dirt", "Unpaved surface");
        createNotes(props, "surface=earth", "Unpaved surface");
        createNotes(props, "surface=grass", "Unpaved surface");
        createNotes(props, "surface=mud", "Unpaved surface -- muddy!");

        /* and some names */
        createNames(props, "otp:route_ref=*", "Route {otp:route_ref}");
        createNames(props, "highway=platform;ref=*", "Platform {ref}");
        createNames(props, "railway=platform;ref=*", "Platform {ref}");

        // Bridges/Tunnels
        createNames(props, "highway=pedestrian;bridge=*", "footbridge");
        createNames(props, "highway=path;bridge=*", "footbridge");
        createNames(props, "highway=footway;bridge=*", "footbridge");

        createNames(props, "highway=pedestrian;tunnel=*", "underpass");
        createNames(props, "highway=path;tunnel=*", "underpass");
        createNames(props, "highway=footway;tunnel=*", "underpass");

        // Basic Mappings
        createNames(props, "highway=motorway", "road");
        createNames(props, "highway=motorway_link", "onramp");
        createNames(props, "highway=trunk", "road");
        createNames(props, "highway=trunk_link", "onramp");

        createNames(props, "highway=primary", "road");
        createNames(props, "highway=primary_link", "road");
        createNames(props, "highway=secondary", "road");
        createNames(props, "highway=secondary_link", "road");
        createNames(props, "highway=tertiary", "road");
        createNames(props, "highway=tertiary_link", "road");
        createNames(props, "highway=unclassified", "road");
        createNames(props, "highway=residential", "road");
        createNames(props, "highway=living_street", "road");
        createNames(props, "highway=road", "road");
        createNames(props, "highway=service", "service road");
        createNames(props, "highway=service;service=parking_aisle", "parking lot");
        createNames(props, "highway=byway", "byway");
        createNames(props, "highway=track", "track");

        createNames(props, "highway=footway;footway=sidewalk", "sidewalk");

        createNames(props, "highway=cycleway", "bike path");
        createNames(props, "cycleway=track", "bike path");
        createNames(props, "highway=pedestrian", "path");
        createNames(props, "highway=path", "path");
        createNames(props, "highway=footway", "path");
        createNames(props, "highway=bridleway", "bridleway");

        createNames(props, "highway=platform", "platform");
        createNames(props, "railway=platform", "platform");

        createNames(props, "highway=steps", "steps");

        //slope overrides
        props.setSlopeOverride(new OSMSpecifier("bridge=*"), true);
        props.setSlopeOverride(new OSMSpecifier("tunnel=*"), true);

        return props;
    }

    private void createNames(WayPropertySet propset, String spec, String pattern) {
        CreativeNamer namer = new CreativeNamer(pattern);
        propset.addCreativeNamer(new OSMSpecifier(spec), namer);
    }

    private void createNotes(WayPropertySet propset, String spec, String pattern) {
        NoteProperties properties = new NoteProperties();
        properties.setNotePattern(pattern);
        propset.addNote(new OSMSpecifier(spec), properties);
    }

    private void setProperties(WayPropertySet propset, String spec,
            StreetTraversalPermission permission) {
        setProperties(propset, spec, permission, 1.0, 1.0);
    }

    private void setProperties(WayPropertySet propset, String spec,
            StreetTraversalPermission permission, double safety, double safetyBack) {
        setProperties(propset, spec, permission, safety, safetyBack, false);
    }

    private void setProperties(WayPropertySet propset, String spec,
            StreetTraversalPermission permission, double safety, double safetyBack, boolean mixin) {
        WayProperties properties = new WayProperties();
        properties.setPermission(permission);
        properties.setSafetyFeatures(new P2<Double>(safety, safetyBack));
        propset.addProperties(new OSMSpecifier(spec), properties, mixin);
    }
}

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

package org.opentripplanner.graph_builder.impl.osm;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory class provides a default collection of {@link WayProperties} that determine how OSM streets can be
 * traversed in various modes.
 *
 * Circa January 2011, Grant and Mele at TriMet undertook proper testing of bike (and transit) routing, and worked
 * with David Turner on assigning proper weights to different facility types. The weights in this file grew organically
 * from trial and error, and are the result of months of testing and tweaking the routes that OTP returned, as well as
 * actually walking/biking these routes and making changes based on those experiences. This set of weights should be
 * a great starting point for others to use, but they are to some extent tailored to the situation in Portland and
 * people shouldn't hesitate to adjust them to for their own instance.
 *
 * The rules for assigning WayProperties to OSM ways are explained in. The final tie breaker if two Pickers both match
 * is the sequence that the properties are added in this file: if all else is equal the 'setProperties' statement that
 * is closer to the top of the page will prevail over those lower down the page.
 *
 * Foot and bicycle permissions are also addressed in OpenStreetMapGraphBuilderImpl.Handler#getPermissionsForEntity().
 * For instance, if a way that normally does not permit walking based on its tag matches (the prevailing 'setProperties'
 * statement) has a 'foot=yes' tag the permissions are overridden and walking is allowed on that way.
 *
 * TODO clarify why this needs a separate factory interface.
 *
 * @author bdferris, novalis
 * @see WayPropertySetSource
 * @see OpenStreetMapGraphBuilderImpl
 */
public class DefaultWayPropertySetSource implements WayPropertySetSource {

    private static Logger LOG = LoggerFactory.getLogger(DefaultWayPropertySetSource.class);

    private Locale locale = Locale.getDefault();

    ResourceBundle resources;

    /* properties and permissions for ways */
    @Override
    public WayPropertySet getWayPropertySet() {
        WayPropertySet props = new WayPropertySet();

        /* no bicycle tags */

        /* NONE */
        setProperties(props, "highway=raceway", StreetTraversalPermission.NONE);
        setProperties(props, "highway=construction", StreetTraversalPermission.NONE);

        /* PEDESTRIAN */
        setProperties(props, "highway=steps", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "highway=crossing", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "highway=platform", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "public_transport=platform", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "railway=platform", StreetTraversalPermission.PEDESTRIAN);
        setProperties(props, "footway=sidewalk;highway=footway",
                StreetTraversalPermission.PEDESTRIAN);

        /* PEDESTRIAN_AND_BICYCLE */
        setProperties(props, "highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                0.60, 0.60);
        setProperties(props, "highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                0.75, 0.75);
        setProperties(props, "highway=pedestrian",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.90, 0.90);
        setProperties(props, "highway=footway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                1.1, 1.1);
        setProperties(props, "highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                1.3, 1.3);

        /* ALL */
        setProperties(props, "highway=living_street", StreetTraversalPermission.ALL, 0.90, 0.90);
        setProperties(props, "highway=unclassified", StreetTraversalPermission.ALL, 1, 1);
        setProperties(props, "highway=road", StreetTraversalPermission.ALL, 1, 1);
        setProperties(props, "highway=byway", StreetTraversalPermission.ALL, 1.3, 1.3);
        setProperties(props, "highway=track", StreetTraversalPermission.ALL, 1.3, 1.3);
        setProperties(props, "highway=service", StreetTraversalPermission.ALL, 1.1, 1.1);
        setProperties(props, "highway=residential", StreetTraversalPermission.ALL, 0.98, 0.98);
        setProperties(props, "highway=residential_link", StreetTraversalPermission.ALL, 0.98, 0.98);
        setProperties(props, "highway=tertiary", StreetTraversalPermission.ALL, 1, 1);
        setProperties(props, "highway=tertiary_link", StreetTraversalPermission.ALL, 1, 1);
        setProperties(props, "highway=secondary", StreetTraversalPermission.ALL, 1.5, 1.5);
        setProperties(props, "highway=secondary_link", StreetTraversalPermission.ALL, 1.5, 1.5);
        setProperties(props, "highway=primary", StreetTraversalPermission.ALL, 2.06, 2.06);
        setProperties(props, "highway=primary_link", StreetTraversalPermission.ALL, 2.06, 2.06);

        /* DRIVING ONLY */
        // trunk and motorway links are often short distances and necessary connections
        setProperties(props, "highway=trunk_link", StreetTraversalPermission.ALL_DRIVING, 2.06,
                2.06);
        setProperties(props, "highway=motorway_link", StreetTraversalPermission.ALL_DRIVING, 2.06,
                2.06);

        setProperties(props, "highway=trunk", StreetTraversalPermission.ALL_DRIVING, 7.47, 7.47);
        setProperties(props, "highway=motorway", StreetTraversalPermission.ALL_DRIVING, 8, 8);

        /* cycleway=lane */
        setProperties(props, "highway=*;cycleway=lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.87, 0.87);
        setProperties(props, "highway=service;cycleway=lane", StreetTraversalPermission.ALL, 0.77,
                0.77);
        setProperties(props, "highway=residential;cycleway=lane", StreetTraversalPermission.ALL,
                0.77, 0.77);
        setProperties(props, "highway=residential_link;cycleway=lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        setProperties(props, "highway=tertiary;cycleway=lane", StreetTraversalPermission.ALL, 0.87,
                0.87);
        setProperties(props, "highway=tertiary_link;cycleway=lane", StreetTraversalPermission.ALL,
                0.87, 0.87);
        setProperties(props, "highway=secondary;cycleway=lane", StreetTraversalPermission.ALL,
                0.96, 0.96);
        setProperties(props, "highway=secondary_link;cycleway=lane", StreetTraversalPermission.ALL,
                0.96, 0.96);
        setProperties(props, "highway=primary;cycleway=lane", StreetTraversalPermission.ALL, 1.15,
                1.15);
        setProperties(props, "highway=primary_link;cycleway=lane", StreetTraversalPermission.ALL,
                1.15, 1.15);

        /* BICYCLE_AND_CAR */
        setProperties(props, "highway=trunk;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 1.5, 1.5);
        setProperties(props, "highway=trunk_link;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 1.15, 1.15);
        setProperties(props, "highway=motorway;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 2, 2);
        setProperties(props, "highway=motorway_link;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 1.15, 1.15);

        /* cycleway=share_busway */
        setProperties(props, "highway=*;cycleway=share_busway",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
        setProperties(props, "highway=service;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.85, 0.85);
        setProperties(props, "highway=residential;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.85, 0.85);
        setProperties(props, "highway=residential_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.85, 0.85);
        setProperties(props, "highway=tertiary;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.92, 0.92);
        setProperties(props, "highway=tertiary_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.92, 0.92);
        setProperties(props, "highway=secondary;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.99, 0.99);
        setProperties(props, "highway=secondary_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.99, 0.99);
        setProperties(props, "highway=primary;cycleway=share_busway",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        setProperties(props, "highway=primary_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        setProperties(props, "highway=trunk;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 1.75, 1.75);
        setProperties(props, "highway=trunk_link;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 1.25, 1.25);
        setProperties(props, "highway=motorway;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 2.5, 2.5);
        setProperties(props, "highway=motorway_link;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 1.25, 1.25);

        /* cycleway=opposite_lane */
        setProperties(props, "highway=*;cycleway=opposite_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.87);
        setProperties(props, "highway=service;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.1, 0.77);
        setProperties(props, "highway=residential;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 0.98, 0.77);
        setProperties(props, "highway=residential_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 0.98, 0.77);
        setProperties(props, "highway=tertiary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1, 0.87);
        setProperties(props, "highway=tertiary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1, 0.87);
        setProperties(props, "highway=secondary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.5, 0.96);
        setProperties(props, "highway=secondary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.5, 0.96);
        setProperties(props, "highway=primary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        setProperties(props, "highway=primary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        setProperties(props, "highway=trunk;cycleway=opposite_lane",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 7.47, 1.5);
        setProperties(props, "highway=trunk_link;cycleway=opposite_lane",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 2.06, 1.15);

        /* cycleway=track */
        setProperties(props, "highway=*;cycleway=track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.75, 0.75);
        setProperties(props, "highway=service;cycleway=track", StreetTraversalPermission.ALL, 0.65,
                0.65);
        setProperties(props, "highway=residential;cycleway=track", StreetTraversalPermission.ALL,
                0.65, 0.65);
        setProperties(props, "highway=residential_link;cycleway=track",
                StreetTraversalPermission.ALL, 0.65, 0.65);
        setProperties(props, "highway=tertiary;cycleway=track", StreetTraversalPermission.ALL,
                0.75, 0.75);
        setProperties(props, "highway=tertiary_link;cycleway=track", StreetTraversalPermission.ALL,
                0.75, 0.75);
        setProperties(props, "highway=secondary;cycleway=track", StreetTraversalPermission.ALL,
                0.8, 0.8);
        setProperties(props, "highway=secondary_link;cycleway=track",
                StreetTraversalPermission.ALL, 0.8, 0.8);
        setProperties(props, "highway=primary;cycleway=track", StreetTraversalPermission.ALL, 0.85,
                0.85);
        setProperties(props, "highway=primary_link;cycleway=track", StreetTraversalPermission.ALL,
                0.85, 0.85);
        setProperties(props, "highway=trunk;cycleway=track",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 0.95, 0.95);
        setProperties(props, "highway=trunk_link;cycleway=track",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 0.85, 0.85);

        /* cycleway=opposite_track */
        setProperties(props, "highway=*;cycleway=opposite_track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.75);
        setProperties(props, "highway=service;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.1, 0.65);
        setProperties(props, "highway=residential;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 0.98, 0.65);
        setProperties(props, "highway=residential_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 0.98, 0.65);
        setProperties(props, "highway=tertiary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1, 0.75);
        setProperties(props, "highway=tertiary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1, 0.75);
        setProperties(props, "highway=secondary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.5, 0.8);
        setProperties(props, "highway=secondary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.5, 0.8);
        setProperties(props, "highway=primary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        setProperties(props, "highway=primary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        setProperties(props, "highway=trunk;cycleway=opposite_track",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 7.47, 0.95);
        setProperties(props, "highway=trunk_link;cycleway=opposite_track",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 2.06, 0.85);

        /* cycleway=shared_lane a.k.a. bike boulevards or neighborhood greenways */
        setProperties(props, "highway=*;cycleway=shared_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 0.77);
        setProperties(props, "highway=service;cycleway=shared_lane", StreetTraversalPermission.ALL,
                0.73, 0.73);
        setProperties(props, "highway=residential;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        setProperties(props, "highway=residential_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        setProperties(props, "highway=tertiary;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.83, 0.83);
        setProperties(props, "highway=tertiary_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.83, 0.83);
        setProperties(props, "highway=secondary;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        setProperties(props, "highway=secondary_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        setProperties(props, "highway=primary;cycleway=shared_lane", StreetTraversalPermission.ALL,
                1.75, 1.75);
        setProperties(props, "highway=primary_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 1.75, 1.75);

        /* cycleway=opposite */
        setProperties(props, "highway=*;cycleway=opposite",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.4);
        setProperties(props, "highway=service;cycleway=opposite", StreetTraversalPermission.ALL,
                1.1, 1.1);
        setProperties(props, "highway=residential;cycleway=opposite",
                StreetTraversalPermission.ALL, 0.98, 0.98);
        setProperties(props, "highway=residential_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 0.98, 0.98);
        setProperties(props, "highway=tertiary;cycleway=opposite", StreetTraversalPermission.ALL,
                1, 1);
        setProperties(props, "highway=tertiary_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 1, 1);
        setProperties(props, "highway=secondary;cycleway=opposite", StreetTraversalPermission.ALL,
                1.5, 1.71);
        setProperties(props, "highway=secondary_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 1.5, 1.71);
        setProperties(props, "highway=primary;cycleway=opposite", StreetTraversalPermission.ALL,
                2.06, 2.99);
        setProperties(props, "highway=primary_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 2.06, 2.99);

        /*
         * path designed for bicycles (should be treated exactly as a cycleway is), this is a multi-use path (MUP)
         */
        setProperties(props, "highway=path;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.60, 0.60);

        /* special cases for footway, pedestrian and bicycles */
        setProperties(props, "highway=footway;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.75, 0.75);
        setProperties(props, "highway=footway;bicycle=yes;area=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
        setProperties(props, "highway=pedestrian;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.75, 0.75);

        /* sidewalk and crosswalk */
        setProperties(props, "footway=sidewalk;highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 2.5, 2.5);
        setProperties(props, "footway=sidewalk;highway=footway;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
        setProperties(props, "highway=footway;footway=crossing",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 2.5, 2.5);
        setProperties(props, "highway=footway;footway=crossing;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);

        /*
         * bicycles on tracks (tracks are defined in OSM as: Roads for agricultural use, gravel roads in the forest etc.; usually unpaved/unsealed but
         * may occasionally apply to paved tracks as well.)
         */
        setProperties(props, "highway=track;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.18, 1.18);
        setProperties(props, "highway=track;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.99, 0.99);
        setProperties(props, "highway=track;bicycle=yes;surface=*",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.18, 1.18);
        setProperties(props, "highway=track;bicycle=designated;surface=*",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.99, 0.99);
        /* this is to avoid double counting since tracks are almost of surface type that is penalized */
        setProperties(props, "highway=track;surface=*",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.3, 1.3);

        /* bicycle=designated, but no bike infrastructure is present */
        setProperties(props, "highway=*;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.97, 0.97);
        setProperties(props, "highway=service;bicycle=designated", StreetTraversalPermission.ALL,
                0.84, 0.84);
        setProperties(props, "highway=residential;bicycle=designated",
                StreetTraversalPermission.ALL, 0.95, 0.95);
        setProperties(props, "highway=residential_link;bicycle=designated",
                StreetTraversalPermission.ALL, 0.95, 0.95);
        setProperties(props, "highway=tertiary;bicycle=designated", StreetTraversalPermission.ALL,
                0.97, 0.97);
        setProperties(props, "highway=tertiary_link;bicycle=designated",
                StreetTraversalPermission.ALL, 0.97, 0.97);
        setProperties(props, "highway=secondary;bicycle=designated", StreetTraversalPermission.ALL,
                1.46, 1.46);
        setProperties(props, "highway=secondary_link;bicycle=designated",
                StreetTraversalPermission.ALL, 1.46, 1.46);
        setProperties(props, "highway=primary;bicycle=designated", StreetTraversalPermission.ALL,
                2, 2);
        setProperties(props, "highway=primary_link;bicycle=designated",
                StreetTraversalPermission.ALL, 2, 2);
        setProperties(props, "highway=trunk;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 7.25, 7.25);
        setProperties(props, "highway=trunk_link;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 2, 2);
        setProperties(props, "highway=motorway;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 7.76, 7.76);
        setProperties(props, "highway=motorway_link;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_DRIVING, 2, 2);

        /*
         * Automobile speeds in the United States: Based on my (mattwigway) personal experience, primarily in California
         */
        setCarSpeed(props, "highway=motorway", 29); // 29 m/s ~= 65 mph
        setCarSpeed(props, "highway=motorway_link", 15); // ~= 35 mph
        setCarSpeed(props, "highway=trunk", 24.6f); // ~= 55 mph
        setCarSpeed(props, "highway=trunk_link", 15); // ~= 35 mph
        setCarSpeed(props, "highway=primary", 20); // ~= 45 mph
        setCarSpeed(props, "highway=primary_link", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=secondary", 15); // ~= 35 mph
        setCarSpeed(props, "highway=secondary_link", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=tertiary", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=tertiary_link", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=living_street", 2.2f); // ~= 5 mph

        // generally, these will not allow cars at all, but the docs say
        // "For roads used mainly/exclusively for pedestrians . . . which may allow access by
        // motorised vehicles only for very limited periods of the day."
        // http://wiki.openstreetmap.org/wiki/Key:highway
        // This of course makes the street network time-dependent
        setCarSpeed(props, "highway=pedestrian", 2.2f); // ~= 5 mph

        setCarSpeed(props, "highway=residential", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=unclassified", 11.2f); // ~= 25 mph
        setCarSpeed(props, "highway=service", 6.7f); // ~= 15 mph
        setCarSpeed(props, "highway=track", 4.5f); // ~= 10 mph
        setCarSpeed(props, "highway=road", 11.2f); // ~= 25 mph

        // default ~= 25 mph
        props.setDefaultSpeed(11.2f);

        /*** special situations ****/

        /*
         * cycleway:left/right=lane/track/shared_lane permutations - no longer needed because left/right matching algorithm does this
         */

        /* cycleway:left=lane */
        /* cycleway:right=track */
        /* cycleway:left=track */
        /* cycleway:right=shared_lane */
        /* cycleway:left=shared_lane */
        /* cycleway:right=lane, cycleway:left=track */
        /* cycleway:right=lane, cycleway:left=shared_lane */
        /* cycleway:right=track, cycleway:left=lane */
        /* cycleway:right=track, cycleway:left=shared_lane */
        /* cycleway:right=shared_lane, cycleway:left=lane */
        /* cycleway:right=shared_lane, cycleway:left=track */

        /* surface=* mixins */

        /*
         * The following tags have been removed from surface weights because they are no more of an impedence to bicycling than a paved surface
         * surface=paving_stones surface=fine_gravel (sounds counter-intuitive but see the definition on the OSM Wiki) surface=tartan (this what
         * running tracks are usually made of)
         */

        setProperties(props, "surface=unpaved", StreetTraversalPermission.ALL, 1.18, 1.18, true);
        setProperties(props, "surface=compacted", StreetTraversalPermission.ALL, 1.18, 1.18, true);
        setProperties(props, "surface=wood", StreetTraversalPermission.ALL, 1.18, 1.18, true);

        setProperties(props, "surface=cobblestone", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        setProperties(props, "surface=cobblestone:flattened", StreetTraversalPermission.ALL, 1.3,
                1.3, true);
        setProperties(props, "surface=grass_paver", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        setProperties(props, "surface=pebblestone", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        // Can be slick if wet, but otherwise not unfavorable to bikes
        setProperties(props, "surface=metal", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        setProperties(props, "surface=ground", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=dirt", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=earth", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=grass", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=mud", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=woodchip", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=gravel", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        setProperties(props, "surface=artifical_turf", StreetTraversalPermission.ALL, 1.5, 1.5,
                true);

        /* sand is deadly for bikes */
        setProperties(props, "surface=sand", StreetTraversalPermission.ALL, 100.0, 100.0, true);

        /* Portland-local mixins */

        /*
         * the RLIS/CCGIS:bicycle=designated mixins are coded out as they are no longer neccessary because of of the bicycle=designated block of code
         * above. This switch makes our weighting system less reliant on tags that aren't generally used by the OSM community, and prevents the double
         * counting that was occuring on streets with both bicycle infrastructure and an RLIS:bicycle=designated tag
         */

        /*
         * setProperties(props, "RLIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
         */
        setProperties(props, "RLIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.45,
                1.45, true);
        setProperties(props, "RLIS:bicycle:right=caution_area", StreetTraversalPermission.ALL,
                1.45, 1.0, true);
        setProperties(props, "RLIS:bicycle:left=caution_area", StreetTraversalPermission.ALL, 1.0,
                1.45, true);
        /*
         * setProperties(props, "CCGIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
         */
        setProperties(props, "CCGIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.45,
                1.45, true);
        setProperties(props, "CCGIS:bicycle:right=caution_area", StreetTraversalPermission.ALL,
                1.45, 1.0, true);
        setProperties(props, "CCGIS:bicycle:left=caution_area", StreetTraversalPermission.ALL, 1.0,
                1.45, true);

        props.defaultProperties.setPermission(StreetTraversalPermission.ALL);

        /* and the notes */
        createNotes(props, "note=*", "{note}");
        createNotes(props, "notes=*", "{notes}");
        createNotes(props, "RLIS:bicycle=caution_area", "note.caution");
        createNotes(props, "CCGIS:bicycle=caution_area", "note.caution");
        createNotes(props, "surface=unpaved", "note.unpaved_surface");
        createNotes(props, "surface=compacted", "note.unpaved_surface");
        createNotes(props, "surface=ground", "note.unpaved_surface");
        createNotes(props, "surface=dirt", "note.unpaved_surface");
        createNotes(props, "surface=earth", "note.unpaved_surface");
        createNotes(props, "surface=grass", "note.unpaved_surface");
        createNotes(props, "surface=mud", "note.muddy_surface");

        /* and some names */
        // Basics
        createNames(props, "highway=cycleway", "name.bike_path");
        createNames(props, "cycleway=track", "name.bike_path");
        createNames(props, "highway=pedestrian", "name.pedestrian_path");
        createNames(props, "highway=pedestrian;area=yes", "name.pedestrian_area");
        createNames(props, "highway=path", "name.path");
        createNames(props, "highway=footway", "name.pedestrian_path");
        createNames(props, "highway=bridleway", "name.bridleway");
        createNames(props, "highway=footway;bicycle=no", "name.pedestrian_path");

        // Platforms
        createNames(props, "otp:route_ref=*", "name.otp_route_ref");
        createNames(props, "highway=platform;ref=*", "name.platform_ref");
        createNames(props, "railway=platform;ref=*", "name.platform_ref");
        createNames(props, "railway=platform;highway=footway;footway=sidewalk", "name.platform");
        createNames(props, "railway=platform;highway=path;path=sidewalk", "name.platform");
        createNames(props, "railway=platform;highway=pedestrian", "name.platform");
        createNames(props, "railway=platform;highway=path", "name.platform");
        createNames(props, "railway=platform;highway=footway", "name.platform");
        createNames(props, "highway=platform", "name.platform");
        createNames(props, "railway=platform", "name.platform");
        createNames(props, "railway=platform;highway=footway;bicycle=no", "name.platform");

        // Bridges/Tunnels
        createNames(props, "highway=pedestrian;bridge=*", "name.footbridge");
        createNames(props, "highway=path;bridge=*", "name.footbridge");
        createNames(props, "highway=footway;bridge=*", "name.footbridge");

        createNames(props, "highway=pedestrian;tunnel=*", "name.underpass");
        createNames(props, "highway=path;tunnel=*", "name.underpass");
        createNames(props, "highway=footway;tunnel=*", "name.underpass");

        // Basic Mappings
        createNames(props, "highway=motorway", "name.road");
        createNames(props, "highway=motorway_link", "name.ramp");
        createNames(props, "highway=trunk", "name.road");
        createNames(props, "highway=trunk_link", "name.ramp");

        createNames(props, "highway=primary", "name.road");
        createNames(props, "highway=primary_link", "name.link");
        createNames(props, "highway=secondary", "name.road");
        createNames(props, "highway=secondary_link", "name.link");
        createNames(props, "highway=tertiary", "name.road");
        createNames(props, "highway=tertiary_link", "name.link");
        createNames(props, "highway=unclassified", "name.road");
        createNames(props, "highway=residential", "name.road");
        createNames(props, "highway=living_street", "name.road");
        createNames(props, "highway=road", "name.road");
        createNames(props, "highway=service", "name.service_road");
        createNames(props, "highway=service;service=alley", "name.alley");
        createNames(props, "highway=service;service=parking_aisle", "name.parking_aisle");
        createNames(props, "highway=byway", "name.byway");
        createNames(props, "highway=track", "name.track");

        createNames(props, "highway=footway;footway=sidewalk", "name.sidewalk");
        createNames(props, "highway=path;path=sidewalk", "name.sidewalk");

        createNames(props, "highway=steps", "name.steps");

        createNames(props, "amenity=bicycle_rental;name=*", "name.bicycle_rental_name");
        createNames(props, "amenity=bicycle_rental", "name.bicycle_rental_station");

        createNames(props, "amenity=parking;name=*", "name.park_and_ride_name");
        createNames(props, "amenity=parking", "name.park_and_ride_station");

        // slope overrides
        props.setSlopeOverride(new OSMSpecifier("bridge=*"), true);
        props.setSlopeOverride(new OSMSpecifier("embankment=*"), true);
        props.setSlopeOverride(new OSMSpecifier("tunnel=*"), true);

        return props;
    }

    private void createNames(WayPropertySet propset, String spec, String patternKey) {
        String pattern = localize(patternKey);
        CreativeNamer namer = new CreativeNamer(pattern);
        propset.addCreativeNamer(new OSMSpecifier(spec), namer);
    }

    private void createNotes(WayPropertySet propset, String spec, String patternKey) {
        NoteProperties properties = new NoteProperties();
        String pattern = localize(patternKey);
        properties.setNotePattern(pattern);
        propset.addNote(new OSMSpecifier(spec), properties);
    }

    private void setProperties(WayPropertySet propset, String spec,
            StreetTraversalPermission permission) {
        setProperties(propset, spec, permission, 1.0, 1.0);
    }

    /**
     * Note that the safeties here will be adjusted such that the safest street has a safety value of 1, with all others scaled proportionately.
     */
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

    private void setCarSpeed(WayPropertySet propset, String spec, float speed) {
        SpeedPicker picker = new SpeedPicker();
        picker.setSpecifier(new OSMSpecifier(spec));
        picker.setSpeed(speed);
        propset.addSpeedPicker(picker);
    }

    private String localize(String key) {
        try {
            String retval = getResourceBundle().getString(key);
            LOG.debug(String.format("Localized '%s' using '%s'", key, retval));
            return retval;
        } catch (MissingResourceException e) {
            LOG.warn("Missing translation for key: " + key);
            return key;
        }
    }

    private ResourceBundle getResourceBundle() {
        if (resources == null) {
            resources = ResourceBundle.getBundle("WayProperties", locale);
        }
        return resources;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}

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
import org.opentripplanner.routing.services.notes.StreetNotesService;

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
 * is the sequence that the properties are added in this file: if all else is equal the 'props.setProperties' statement that
 * is closer to the top of the page will prevail over those lower down the page.
 *
 * Foot and bicycle permissions are also addressed in OpenStreetMapGraphBuilderImpl.Handler#getPermissionsForEntity().
 * For instance, if a way that normally does not permit walking based on its tag matches (the prevailing 'props.setProperties'
 * statement) has a 'foot=yes' tag the permissions are overridden and walking is allowed on that way.
 *
 * TODO clarify why this needs a separate factory interface.
 *
 * @author bdferris, novalis
 * @see WayPropertySetSource
 * @see OpenStreetMapModule
 */
public class DefaultWayPropertySetSource implements WayPropertySetSource {

    /* Populate properties on existing WayPropertySet */
    public void populateProperties(WayPropertySet props) {
        /* no bicycle tags */

        /* NONE */
        props.setProperties("highway=raceway", StreetTraversalPermission.NONE);
        props.setProperties("highway=construction", StreetTraversalPermission.NONE);

        /* PEDESTRIAN */
		props.setProperties("highway=corridor", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("highway=steps", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("highway=crossing", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("highway=platform", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("public_transport=platform", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("railway=platform", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("footway=sidewalk;highway=footway",
                StreetTraversalPermission.PEDESTRIAN);

        /* PEDESTRIAN_AND_BICYCLE */
        props.setProperties("highway=cycleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                0.60, 0.60);
        props.setProperties("highway=path", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                0.75, 0.75);
        props.setProperties("highway=pedestrian",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.90, 0.90);
        props.setProperties("highway=footway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                1.1, 1.1);
        props.setProperties("highway=bridleway", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
                1.3, 1.3);

        /* ALL */
        props.setProperties("highway=living_street", StreetTraversalPermission.ALL, 0.90, 0.90);
        props.setProperties("highway=unclassified", StreetTraversalPermission.ALL, 1, 1);
        props.setProperties("highway=road", StreetTraversalPermission.ALL, 1, 1);
        props.setProperties("highway=byway", StreetTraversalPermission.ALL, 1.3, 1.3);
        props.setProperties("highway=track", StreetTraversalPermission.ALL, 1.3, 1.3);
        props.setProperties("highway=service", StreetTraversalPermission.ALL, 1.1, 1.1);
        props.setProperties("highway=residential", StreetTraversalPermission.ALL, 0.98, 0.98);
        props.setProperties("highway=residential_link", StreetTraversalPermission.ALL, 0.98, 0.98);
        props.setProperties("highway=tertiary", StreetTraversalPermission.ALL, 1, 1);
        props.setProperties("highway=tertiary_link", StreetTraversalPermission.ALL, 1, 1);
        props.setProperties("highway=secondary", StreetTraversalPermission.ALL, 1.5, 1.5);
        props.setProperties("highway=secondary_link", StreetTraversalPermission.ALL, 1.5, 1.5);
        props.setProperties("highway=primary", StreetTraversalPermission.ALL, 2.06, 2.06);
        props.setProperties("highway=primary_link", StreetTraversalPermission.ALL, 2.06, 2.06);

        /* DRIVING ONLY */
        // trunk and motorway links are often short distances and necessary connections
        props.setProperties("highway=trunk_link", StreetTraversalPermission.CAR, 2.06,
                2.06);
        props.setProperties("highway=motorway_link", StreetTraversalPermission.CAR, 2.06,
                2.06);

        props.setProperties("highway=trunk", StreetTraversalPermission.CAR, 7.47, 7.47);
        props.setProperties("highway=motorway", StreetTraversalPermission.CAR, 8, 8);

        /* cycleway=lane */
        props.setProperties("highway=*;cycleway=lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.87, 0.87);
        props.setProperties("highway=service;cycleway=lane", StreetTraversalPermission.ALL, 0.77,
                0.77);
        props.setProperties("highway=residential;cycleway=lane", StreetTraversalPermission.ALL,
                0.77, 0.77);
        props.setProperties("highway=residential_link;cycleway=lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        props.setProperties("highway=tertiary;cycleway=lane", StreetTraversalPermission.ALL, 0.87,
                0.87);
        props.setProperties("highway=tertiary_link;cycleway=lane", StreetTraversalPermission.ALL,
                0.87, 0.87);
        props.setProperties("highway=secondary;cycleway=lane", StreetTraversalPermission.ALL,
                0.96, 0.96);
        props.setProperties("highway=secondary_link;cycleway=lane", StreetTraversalPermission.ALL,
                0.96, 0.96);
        props.setProperties("highway=primary;cycleway=lane", StreetTraversalPermission.ALL, 1.15,
                1.15);
        props.setProperties("highway=primary_link;cycleway=lane", StreetTraversalPermission.ALL,
                1.15, 1.15);

        /* BICYCLE_AND_CAR */
        props.setProperties("highway=trunk;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.5, 1.5);
        props.setProperties("highway=trunk_link;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.15, 1.15);
        props.setProperties("highway=motorway;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 2, 2);
        props.setProperties("highway=motorway_link;cycleway=lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.15, 1.15);

        /* cycleway=share_busway */
        props.setProperties("highway=*;cycleway=share_busway",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.92, 0.92);
        props.setProperties("highway=service;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.85, 0.85);
        props.setProperties("highway=residential;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.85, 0.85);
        props.setProperties("highway=residential_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.85, 0.85);
        props.setProperties("highway=tertiary;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.92, 0.92);
        props.setProperties("highway=tertiary_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.92, 0.92);
        props.setProperties("highway=secondary;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.99, 0.99);
        props.setProperties("highway=secondary_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 0.99, 0.99);
        props.setProperties("highway=primary;cycleway=share_busway",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        props.setProperties("highway=primary_link;cycleway=share_busway",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        props.setProperties("highway=trunk;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.75, 1.75);
        props.setProperties("highway=trunk_link;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.25, 1.25);
        props.setProperties("highway=motorway;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_CAR, 2.5, 2.5);
        props.setProperties("highway=motorway_link;cycleway=share_busway",
                StreetTraversalPermission.BICYCLE_AND_CAR, 1.25, 1.25);

        /* cycleway=opposite_lane */
        props.setProperties("highway=*;cycleway=opposite_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.87);
        props.setProperties("highway=service;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.1, 0.77);
        props.setProperties("highway=residential;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 0.98, 0.77);
        props.setProperties("highway=residential_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 0.98, 0.77);
        props.setProperties("highway=tertiary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1, 0.87);
        props.setProperties("highway=tertiary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1, 0.87);
        props.setProperties("highway=secondary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.5, 0.96);
        props.setProperties("highway=secondary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 1.5, 0.96);
        props.setProperties("highway=primary;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        props.setProperties("highway=primary_link;cycleway=opposite_lane",
                StreetTraversalPermission.ALL, 2.06, 1.15);
        props.setProperties("highway=trunk;cycleway=opposite_lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 7.47, 1.5);
        props.setProperties("highway=trunk_link;cycleway=opposite_lane",
                StreetTraversalPermission.BICYCLE_AND_CAR, 2.06, 1.15);

        /* cycleway=track */
        props.setProperties("highway=*;cycleway=track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.75, 0.75);
        props.setProperties("highway=service;cycleway=track", StreetTraversalPermission.ALL, 0.65,
                0.65);
        props.setProperties("highway=residential;cycleway=track", StreetTraversalPermission.ALL,
                0.65, 0.65);
        props.setProperties("highway=residential_link;cycleway=track",
                StreetTraversalPermission.ALL, 0.65, 0.65);
        props.setProperties("highway=tertiary;cycleway=track", StreetTraversalPermission.ALL,
                0.75, 0.75);
        props.setProperties("highway=tertiary_link;cycleway=track", StreetTraversalPermission.ALL,
                0.75, 0.75);
        props.setProperties("highway=secondary;cycleway=track", StreetTraversalPermission.ALL,
                0.8, 0.8);
        props.setProperties("highway=secondary_link;cycleway=track",
                StreetTraversalPermission.ALL, 0.8, 0.8);
        props.setProperties("highway=primary;cycleway=track", StreetTraversalPermission.ALL, 0.85,
                0.85);
        props.setProperties("highway=primary_link;cycleway=track", StreetTraversalPermission.ALL,
                0.85, 0.85);
        props.setProperties("highway=trunk;cycleway=track",
                StreetTraversalPermission.BICYCLE_AND_CAR, 0.95, 0.95);
        props.setProperties("highway=trunk_link;cycleway=track",
                StreetTraversalPermission.BICYCLE_AND_CAR, 0.85, 0.85);

        /* cycleway=opposite_track */
        props.setProperties("highway=*;cycleway=opposite_track",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 0.75);
        props.setProperties("highway=service;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.1, 0.65);
        props.setProperties("highway=residential;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 0.98, 0.65);
        props.setProperties("highway=residential_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 0.98, 0.65);
        props.setProperties("highway=tertiary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1, 0.75);
        props.setProperties("highway=tertiary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1, 0.75);
        props.setProperties("highway=secondary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.5, 0.8);
        props.setProperties("highway=secondary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 1.5, 0.8);
        props.setProperties("highway=primary;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        props.setProperties("highway=primary_link;cycleway=opposite_track",
                StreetTraversalPermission.ALL, 2.06, 0.85);
        props.setProperties("highway=trunk;cycleway=opposite_track",
                StreetTraversalPermission.BICYCLE_AND_CAR, 7.47, 0.95);
        props.setProperties("highway=trunk_link;cycleway=opposite_track",
                StreetTraversalPermission.BICYCLE_AND_CAR, 2.06, 0.85);

        /* cycleway=shared_lane a.k.a. bike boulevards or neighborhood greenways */
        props.setProperties("highway=*;cycleway=shared_lane",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.77, 0.77);
        props.setProperties("highway=service;cycleway=shared_lane", StreetTraversalPermission.ALL,
                0.73, 0.73);
        props.setProperties("highway=residential;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        props.setProperties("highway=residential_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.77, 0.77);
        props.setProperties("highway=tertiary;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.83, 0.83);
        props.setProperties("highway=tertiary_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 0.83, 0.83);
        props.setProperties("highway=secondary;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        props.setProperties("highway=secondary_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 1.25, 1.25);
        props.setProperties("highway=primary;cycleway=shared_lane", StreetTraversalPermission.ALL,
                1.75, 1.75);
        props.setProperties("highway=primary_link;cycleway=shared_lane",
                StreetTraversalPermission.ALL, 1.75, 1.75);

        /* cycleway=opposite */
        props.setProperties("highway=*;cycleway=opposite",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.4);
        props.setProperties("highway=service;cycleway=opposite", StreetTraversalPermission.ALL,
                1.1, 1.1);
        props.setProperties("highway=residential;cycleway=opposite",
                StreetTraversalPermission.ALL, 0.98, 0.98);
        props.setProperties("highway=residential_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 0.98, 0.98);
        props.setProperties("highway=tertiary;cycleway=opposite", StreetTraversalPermission.ALL,
                1, 1);
        props.setProperties("highway=tertiary_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 1, 1);
        props.setProperties("highway=secondary;cycleway=opposite", StreetTraversalPermission.ALL,
                1.5, 1.71);
        props.setProperties("highway=secondary_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 1.5, 1.71);
        props.setProperties("highway=primary;cycleway=opposite", StreetTraversalPermission.ALL,
                2.06, 2.99);
        props.setProperties("highway=primary_link;cycleway=opposite",
                StreetTraversalPermission.ALL, 2.06, 2.99);

        /*
         * path designed for bicycles (should be treated exactly as a cycleway is), this is a multi-use path (MUP)
         */
        props.setProperties("highway=path;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.60, 0.60);

        /* special cases for footway, pedestrian and bicycles */
        props.setProperties("highway=footway;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.75, 0.75);
        props.setProperties("highway=footway;bicycle=yes;area=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.9, 0.9);
        props.setProperties("highway=pedestrian;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.75, 0.75);

        /* sidewalk and crosswalk */
        props.setProperties("footway=sidewalk;highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 2.5, 2.5);
        props.setProperties("footway=sidewalk;highway=footway;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);
        props.setProperties("highway=footway;footway=crossing",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 2.5, 2.5);
        props.setProperties("highway=footway;footway=crossing;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.1, 1.1);

        /*
         * bicycles on tracks (tracks are defined in OSM as: Roads for agricultural use, gravel roads in the forest etc.; usually unpaved/unsealed but
         * may occasionally apply to paved tracks as well.)
         */
        props.setProperties("highway=track;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.18, 1.18);
        props.setProperties("highway=track;bicycle=designated",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.99, 0.99);
        props.setProperties("highway=track;bicycle=yes;surface=*",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.18, 1.18);
        props.setProperties("highway=track;bicycle=designated;surface=*",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.99, 0.99);
        /* this is to avoid double counting since tracks are almost of surface type that is penalized */
        props.setProperties("highway=track;surface=*",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.3, 1.3);

        /* bicycle=designated, but no bike infrastructure is present */
        props.setProperties("highway=*;bicycle=designated",
                StreetTraversalPermission.ALL, 0.97, 0.97);
        props.setProperties("highway=service;bicycle=designated", StreetTraversalPermission.ALL,
                0.84, 0.84);
        props.setProperties("highway=residential;bicycle=designated",
                StreetTraversalPermission.ALL, 0.95, 0.95);
        props.setProperties("highway=unclassified;bicycle=designated",
                StreetTraversalPermission.ALL, 0.95, 0.95);
        props.setProperties("highway=residential_link;bicycle=designated",
                StreetTraversalPermission.ALL, 0.95, 0.95);
        props.setProperties("highway=tertiary;bicycle=designated", StreetTraversalPermission.ALL,
                0.97, 0.97);
        props.setProperties("highway=tertiary_link;bicycle=designated",
                StreetTraversalPermission.ALL, 0.97, 0.97);
        props.setProperties("highway=secondary;bicycle=designated", StreetTraversalPermission.ALL,
                1.46, 1.46);
        props.setProperties("highway=secondary_link;bicycle=designated",
                StreetTraversalPermission.ALL, 1.46, 1.46);
        props.setProperties("highway=primary;bicycle=designated", StreetTraversalPermission.ALL,
                2, 2);
        props.setProperties("highway=primary_link;bicycle=designated",
                StreetTraversalPermission.ALL, 2, 2);
        props.setProperties("highway=trunk;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_CAR, 7.25, 7.25);
        props.setProperties("highway=trunk_link;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_CAR, 2, 2);
        props.setProperties("highway=motorway;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_CAR, 7.76, 7.76);
        props.setProperties("highway=motorway_link;bicycle=designated",
                StreetTraversalPermission.BICYCLE_AND_CAR, 2, 2);

        /*
         * Automobile speeds in the United States: Based on my (mattwigway) personal experience, primarily in California
         */
        props.setCarSpeed("highway=motorway", 29); // 29 m/s ~= 65 mph
        props.setCarSpeed("highway=motorway_link", 15); // ~= 35 mph
        props.setCarSpeed("highway=trunk", 24.6f); // ~= 55 mph
        props.setCarSpeed("highway=trunk_link", 15); // ~= 35 mph
        props.setCarSpeed("highway=primary", 20); // ~= 45 mph
        props.setCarSpeed("highway=primary_link", 11.2f); // ~= 25 mph
        props.setCarSpeed("highway=secondary", 15); // ~= 35 mph
        props.setCarSpeed("highway=secondary_link", 11.2f); // ~= 25 mph
        props.setCarSpeed("highway=tertiary", 11.2f); // ~= 25 mph
        props.setCarSpeed("highway=tertiary_link", 11.2f); // ~= 25 mph
        props.setCarSpeed("highway=living_street", 2.2f); // ~= 5 mph

        // generally, these will not allow cars at all, but the docs say
        // "For roads used mainly/exclusively for pedestrians . . . which may allow access by
        // motorised vehicles only for very limited periods of the day."
        // http://wiki.openstreetmap.org/wiki/Key:highway
        // This of course makes the street network time-dependent
        props.setCarSpeed("highway=pedestrian", 2.2f); // ~= 5 mph

        props.setCarSpeed("highway=residential", 11.2f); // ~= 25 mph
        props.setCarSpeed("highway=unclassified", 11.2f); // ~= 25 mph
        props.setCarSpeed("highway=service", 6.7f); // ~= 15 mph
        props.setCarSpeed("highway=track", 4.5f); // ~= 10 mph
        props.setCarSpeed("highway=road", 11.2f); // ~= 25 mph

        // default ~= 25 mph
        props.defaultSpeed = 11.2f;

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

        props.setProperties("surface=unpaved", StreetTraversalPermission.ALL, 1.18, 1.18, true);
        props.setProperties("surface=compacted", StreetTraversalPermission.ALL, 1.18, 1.18, true);
        props.setProperties("surface=wood", StreetTraversalPermission.ALL, 1.18, 1.18, true);

        props.setProperties("surface=cobblestone", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        props.setProperties("surface=cobblestone:flattened", StreetTraversalPermission.ALL, 1.3,
                1.3, true);
        props.setProperties("surface=grass_paver", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        props.setProperties("surface=pebblestone", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        // Can be slick if wet, but otherwise not unfavorable to bikes
        props.setProperties("surface=metal", StreetTraversalPermission.ALL, 1.3, 1.3, true);
        props.setProperties("surface=ground", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=dirt", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=earth", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=grass", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=mud", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=woodchip", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=gravel", StreetTraversalPermission.ALL, 1.5, 1.5, true);
        props.setProperties("surface=artifical_turf", StreetTraversalPermission.ALL, 1.5, 1.5,
                true);

        /* sand is deadly for bikes */
        props.setProperties("surface=sand", StreetTraversalPermission.ALL, 100.0, 100.0, true);

        /* Portland-local mixins */

        /*
         * the RLIS/CCGIS:bicycle=designated mixins are coded out as they are no longer neccessary because of of the bicycle=designated block of code
         * above. This switch makes our weighting system less reliant on tags that aren't generally used by the OSM community, and prevents the double
         * counting that was occuring on streets with both bicycle infrastructure and an RLIS:bicycle=designated tag
         */

        /*
         * props.setProperties("RLIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
         */
        props.setProperties("RLIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.45,
                1.45, true);
        props.setProperties("RLIS:bicycle:right=caution_area", StreetTraversalPermission.ALL,
                1.45, 1.0, true);
        props.setProperties("RLIS:bicycle:left=caution_area", StreetTraversalPermission.ALL, 1.0,
                1.45, true);
        /*
         * props.setProperties("CCGIS:bicycle=designated", StreetTraversalPermission.ALL, 0.97, 0.97, true);
         */
        props.setProperties("CCGIS:bicycle=caution_area", StreetTraversalPermission.ALL, 1.45,
                1.45, true);
        props.setProperties("CCGIS:bicycle:right=caution_area", StreetTraversalPermission.ALL,
                1.45, 1.0, true);
        props.setProperties("CCGIS:bicycle:left=caution_area", StreetTraversalPermission.ALL, 1.0,
                1.45, true);

        props.defaultProperties.setPermission(StreetTraversalPermission.ALL);

        /* and the notes */
        // TODO: The curly brackets in the string below mean that the CreativeNamer should substitute in OSM tag values.
        // However they are not taken into account when passed to the translation function.
        // props.createNotes("wheelchair:description=*", "{wheelchair:description}", StreetNotesService.WHEELCHAIR_MATCHER);
        // TODO: The two entries below produce lots of spurious notes (because of OSM mapper comments)
        // props.createNotes("note=*", "{note}", StreetNotesService.ALWAYS_MATCHER);
        // props.createNotes("notes=*", "{notes}", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("RLIS:bicycle=caution_area", "note.caution", StreetNotesService.BICYCLE_MATCHER);
        props.createNotes("CCGIS:bicycle=caution_area", "note.caution", StreetNotesService.BICYCLE_MATCHER);
        // TODO: Maybe we should apply the following notes only for car/bike
        props.createNotes("surface=unpaved", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("surface=compacted", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("surface=ground", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("surface=dirt", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("surface=earth", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("surface=grass", "note.unpaved_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("surface=mud", "note.muddy_surface", StreetNotesService.ALWAYS_MATCHER);
        props.createNotes("toll=yes", "note.toll", StreetNotesService.DRIVING_MATCHER);
        props.createNotes("toll:motorcar=yes", "note.toll", StreetNotesService.DRIVING_MATCHER);

        /* and some names */
        // Basics
        props.createNames("highway=cycleway", "name.bike_path");
        props.createNames("cycleway=track", "name.bike_path");
        props.createNames("highway=pedestrian", "name.pedestrian_path");
        props.createNames("highway=pedestrian;area=yes", "name.pedestrian_area");
        props.createNames("highway=path", "name.path");
        props.createNames("highway=footway", "name.pedestrian_path");
        props.createNames("highway=bridleway", "name.bridleway");
        props.createNames("highway=footway;bicycle=no", "name.pedestrian_path");

        // Platforms
        props.createNames("otp:route_ref=*", "name.otp_route_ref");
        props.createNames("highway=platform;ref=*", "name.platform_ref");
        props.createNames("railway=platform;ref=*", "name.platform_ref");
        props.createNames("railway=platform;highway=footway;footway=sidewalk", "name.platform");
        props.createNames("railway=platform;highway=path;path=sidewalk", "name.platform");
        props.createNames("railway=platform;highway=pedestrian", "name.platform");
        props.createNames("railway=platform;highway=path", "name.platform");
        props.createNames("railway=platform;highway=footway", "name.platform");
        props.createNames("highway=platform", "name.platform");
        props.createNames("railway=platform", "name.platform");
        props.createNames("railway=platform;highway=footway;bicycle=no", "name.platform");

        // Bridges/Tunnels
        props.createNames("highway=pedestrian;bridge=*", "name.footbridge");
        props.createNames("highway=path;bridge=*", "name.footbridge");
        props.createNames("highway=footway;bridge=*", "name.footbridge");

        props.createNames("highway=pedestrian;tunnel=*", "name.underpass");
        props.createNames("highway=path;tunnel=*", "name.underpass");
        props.createNames("highway=footway;tunnel=*", "name.underpass");

        // Basic Mappings
        props.createNames("highway=motorway", "name.road");
        props.createNames("highway=motorway_link", "name.ramp");
        props.createNames("highway=trunk", "name.road");
        props.createNames("highway=trunk_link", "name.ramp");

        props.createNames("highway=primary", "name.road");
        props.createNames("highway=primary_link", "name.link");
        props.createNames("highway=secondary", "name.road");
        props.createNames("highway=secondary_link", "name.link");
        props.createNames("highway=tertiary", "name.road");
        props.createNames("highway=tertiary_link", "name.link");
        props.createNames("highway=unclassified", "name.road");
        props.createNames("highway=residential", "name.road");
        props.createNames("highway=living_street", "name.road");
        props.createNames("highway=road", "name.road");
        props.createNames("highway=service", "name.service_road");
        props.createNames("highway=service;service=alley", "name.alley");
        props.createNames("highway=service;service=parking_aisle", "name.parking_aisle");
        props.createNames("highway=byway", "name.byway");
        props.createNames("highway=track", "name.track");

        props.createNames("highway=footway;footway=sidewalk", "name.sidewalk");
        props.createNames("highway=path;path=sidewalk", "name.sidewalk");

        props.createNames("highway=steps", "name.steps");

        props.createNames("amenity=bicycle_parking;name=*", "name.bicycle_parking_name");
        props.createNames("amenity=bicycle_parking", "name.bicycle_parking");

        props.createNames("amenity=parking;name=*", "name.park_and_ride_name");
        props.createNames("amenity=parking", "name.park_and_ride_station");

        // slope overrides
        props.setSlopeOverride(new OSMSpecifier("bridge=*"), true);
        props.setSlopeOverride(new OSMSpecifier("embankment=*"), true);
        props.setSlopeOverride(new OSMSpecifier("tunnel=*"), true);

    }
}

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

import org.junit.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

import junit.framework.TestCase;

/**
 * Test the WayPropertySet 
 * @author mattwigway
 */
public class TestWayPropertySet extends TestCase {
    /**
     * Test that two values are within epsilon of each other.
     * @param val1
     * @param val2
     * @param epsilon
     */
    private boolean within(float val1, float val2, float epsilon) {
        return (Math.abs(val1 - val2) < epsilon);
    }
    
    /**
     * Convert kilometers per hour to meters per second
     * @param kmh
     * @return
     */
    private float kmhAsMs (float kmh) {
        return kmh * 1000 / 3600;
    }
    
    /**
     * Convenience function to get a speed picker for a given specifier and speed.
     * @param specifier The OSM specifier, e.g. highway=motorway
     * @param speed The speed, in meters per second
     */
    private SpeedPicker getSpeedPicker (String specifier, float speed) {
        SpeedPicker sp = new SpeedPicker();
        sp.specifier = new OSMSpecifier(specifier);
        sp.speed = speed;
        return sp;
    }
    
    /**
     * Test that car speeds are calculated accurately
     */
    @Test
    public void testCarSpeeds () {
       DefaultWayPropertySetSource source = new DefaultWayPropertySetSource();
       WayPropertySet wps = source.getWayPropertySet();
       
       OSMWithTags way;
       
       float epsilon = 0.01f;
       
       way = new OSMWithTags();
       way.addTag("maxspeed", "60");
       assertTrue(within(kmhAsMs(60), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(60), wps.getCarSpeedForWay(way, true), epsilon));
       
       way = new OSMWithTags();
       way.addTag("maxspeed:forward", "80");
       way.addTag("maxspeed:reverse", "20");
       way.addTag("maxspeed", "40");
       assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(20), wps.getCarSpeedForWay(way, true), epsilon));
       
       way = new OSMWithTags();
       way.addTag("maxspeed", "40");
       way.addTag("maxspeed:lanes", "60|80|40");
       assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, true), epsilon));
       
       way = new OSMWithTags();
       way.addTag("maxspeed", "20");
       way.addTag("maxspeed:motorcar", "80");
       assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, true), epsilon));
       
       // test with english units
       way = new OSMWithTags();
       way.addTag("maxspeed", "35 mph");
       assertTrue(within(kmhAsMs(35 * 1.609f), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(35 * 1.609f), wps.getCarSpeedForWay(way, true), epsilon));
       
       // test with no maxspeed tags
       wps = new WayPropertySet();
       wps.addSpeedPicker(getSpeedPicker("highway=motorway", kmhAsMs(100)));
       wps.addSpeedPicker(getSpeedPicker("highway=*", kmhAsMs(35)));
       wps.addSpeedPicker(getSpeedPicker("surface=gravel", kmhAsMs(10)));
       wps.defaultSpeed = kmhAsMs(25);
       
       way = new OSMWithTags();
     
       // test default speeds
       assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, true), epsilon));
       
       way.addTag("highway", "tertiary");
       assertTrue(within(kmhAsMs(35), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(35), wps.getCarSpeedForWay(way, true), epsilon));
       
       way = new OSMWithTags();
       way.addTag("surface", "gravel");
       assertTrue(within(kmhAsMs(10), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(10), wps.getCarSpeedForWay(way, true), epsilon));
       
       way = new OSMWithTags();
       way.addTag("highway", "motorway");
       assertTrue(within(kmhAsMs(100), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(100), wps.getCarSpeedForWay(way, true), epsilon));
       
       // make sure that 0-speed ways can't exist
       way = new OSMWithTags();
       way.addTag("maxspeed", "0");
       assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, false), epsilon));
       assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, true), epsilon));
    }
}

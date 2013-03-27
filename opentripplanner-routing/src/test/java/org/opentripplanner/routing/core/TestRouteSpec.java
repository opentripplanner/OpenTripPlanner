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

package org.opentripplanner.routing.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class TestRouteSpec extends TestCase {

    public void testRouteSpec() {

        RouteSpec routeSpec = new RouteSpec("A_01");
        assertEquals("A", routeSpec.agency);
        assertEquals("01", routeSpec.routeName);
        assertNull(routeSpec.routeId);

        routeSpec = new RouteSpec("A__42");
        assertEquals("A", routeSpec.agency);
        assertNull(routeSpec.routeName);
        assertEquals("42", routeSpec.routeId);

        List<RouteSpec> routeSpecs = RouteSpec.listFromString("B_02,B__43,");
        assertEquals(2, routeSpecs.size());
        assertEquals("B", routeSpecs.get(0).agency);
        assertEquals("02", routeSpecs.get(0).routeName);
        assertNull(routeSpecs.get(0).routeId);
        assertEquals("B", routeSpecs.get(1).agency);
        assertNull(routeSpecs.get(1).routeName);
        assertEquals("43", routeSpecs.get(1).routeId);

        RouteSpec routeSpec2 = new RouteSpec("A", "01", "42");
        routeSpec = new RouteSpec("A_01");
        assertTrue(routeSpec.equals(routeSpec2));
        assertTrue(routeSpec2.equals(routeSpec));
        routeSpec = new RouteSpec("A__42");
        assertTrue(routeSpec.equals(routeSpec2));
        assertTrue(routeSpec2.equals(routeSpec));
        routeSpec = new RouteSpec("A_01_42");
        assertTrue(routeSpec.equals(routeSpec2));
        assertTrue(routeSpec2.equals(routeSpec));
        routeSpec = new RouteSpec("A_02");
        assertFalse(routeSpec.equals(routeSpec2));
        assertFalse(routeSpec2.equals(routeSpec));
        routeSpec = new RouteSpec("A__43");
        assertFalse(routeSpec.equals(routeSpec2));
        assertFalse(routeSpec2.equals(routeSpec));

        routeSpec2 = new RouteSpec("A", null, "42");
        routeSpec = new RouteSpec("A", "01");
        assertFalse(routeSpec.equals(routeSpec2));

        Set<RouteSpec> routeSpecMap = new HashSet<RouteSpec>();
        routeSpecMap.add(new RouteSpec("A_01"));
        routeSpecMap.add(new RouteSpec("A__42"));
        routeSpecMap.add(new RouteSpec("A_01"));
        assertEquals(2, routeSpecMap.size());
        assertTrue(routeSpecMap.contains(new RouteSpec("A", "01", null)));
        assertTrue(routeSpecMap.contains(new RouteSpec("A", null, "42")));
        assertTrue(routeSpecMap.contains(new RouteSpec("A", "01", "42")));
        assertTrue(routeSpecMap.contains(new RouteSpec("A", "02", "42")));
        assertTrue(routeSpecMap.contains(new RouteSpec("A", "01", "43")));
        assertFalse(routeSpecMap.contains(new RouteSpec("A", "02", "43")));

    }

}

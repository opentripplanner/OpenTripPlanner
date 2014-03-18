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

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;

public class TestRouteMatcher extends TestCase {

    public void testRouteMatcher() {

        Route r1 = new Route();
        r1.setId(new AgencyAndId("A1", "42"));
        r1.setShortName("R1");
        Route r2 = new Route();
        r2.setId(new AgencyAndId("A1", "43"));
        r2.setShortName("R2");
        Route r1b = new Route();
        r1b.setId(new AgencyAndId("A2", "42"));
        r1b.setShortName("R1");
        Route r3 = new Route();
        r3.setId(new AgencyAndId("A1", "44"));
        r3.setShortName("R3_b");

        RouteMatcher emptyMatcher = RouteMatcher.emptyMatcher();
        assertFalse(emptyMatcher.matches(r1));
        assertFalse(emptyMatcher.matches(r1b));
        assertFalse(emptyMatcher.matches(r2));

        RouteMatcher matcherR1i = RouteMatcher.parse("A1__42");
        assertTrue(matcherR1i.matches(r1));
        assertFalse(matcherR1i.matches(r1b));
        assertFalse(matcherR1i.matches(r2));

        RouteMatcher matcherR2n = RouteMatcher.parse("A1_R2");
        assertFalse(matcherR2n.matches(r1));
        assertFalse(matcherR2n.matches(r1b));
        assertTrue(matcherR2n.matches(r2));

        RouteMatcher matcherR1R2 = RouteMatcher.parse("A1_R1,A1__43,A2__43");
        assertTrue(matcherR1R2.matches(r1));
        assertFalse(matcherR1R2.matches(r1b));
        assertTrue(matcherR1R2.matches(r2));

        RouteMatcher matcherR1n = RouteMatcher.parse("_R1");
        assertTrue(matcherR1n.matches(r1));
        assertTrue(matcherR1n.matches(r1b));
        assertFalse(matcherR1n.matches(r2));

        RouteMatcher matcherR1R1bR2 = RouteMatcher.parse("A1_R1,A2_R1,A1_R2");
        assertTrue(matcherR1R1bR2.matches(r1));
        assertTrue(matcherR1R1bR2.matches(r1b));
        assertTrue(matcherR1R1bR2.matches(r2));

        RouteMatcher matcherR3e = RouteMatcher.parse("A1_R3 b");
        assertFalse(matcherR3e.matches(r1));
        assertFalse(matcherR3e.matches(r1b));
        assertFalse(matcherR3e.matches(r2));
        assertTrue(matcherR3e.matches(r3));

        RouteMatcher nullList = RouteMatcher.parse(null);
        assertTrue(nullList == RouteMatcher.emptyMatcher());

        RouteMatcher emptyList = RouteMatcher.parse("");
        assertTrue(emptyList == RouteMatcher.emptyMatcher());

        RouteMatcher degenerate = RouteMatcher.parse(",,,");
        assertTrue(degenerate == RouteMatcher.emptyMatcher());

        boolean thrown = false;
        try {
            RouteMatcher badMatcher = RouteMatcher.parse("A1_R1_42");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
        
        Route r1c = new Route();
        r1c.setId(new AgencyAndId("A_1", "R_42"));
        r1c.setShortName("R_42");

        RouteMatcher matcherR1c = RouteMatcher.parse("A\\_1_R 42");
        assertTrue(matcherR1c.matches(r1c));
        assertFalse(matcherR1c.matches(r1));
        assertFalse(matcherR1c.matches(r1b));

        RouteMatcher matcherR1c2 = RouteMatcher.parse("A\\_1__R\\_42");
        assertTrue(matcherR1c2.matches(r1c));
        assertFalse(matcherR1c2.matches(r1));
        assertFalse(matcherR1c2.matches(r1b));
    }

}

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
import org.onebusaway.gtfs.model.Stop;

public class TestStopMatcher extends TestCase {

    public void testStopMatcher() {

        Stop s1 = new Stop();
        s1.setId(new AgencyAndId("A1", "42"));
        Stop s2 = new Stop();
        s2.setId(new AgencyAndId("A1", "43"));

        StopMatcher emptyMatcher = StopMatcher.emptyMatcher();
        assertFalse(emptyMatcher.matches(s1));
        assertFalse(emptyMatcher.matches(s2));

        StopMatcher matcherS1 = StopMatcher.parse("A1_42");
        assertTrue(matcherS1.matches(s1));
        assertFalse(matcherS1.matches(s2));

        StopMatcher matcherS2 = StopMatcher.parse("A1_43");
        assertFalse(matcherS2.matches(s1));
        assertTrue(matcherS2.matches(s2));

        StopMatcher matcherS1S2 = StopMatcher.parse("A1_42,A1_43");
        assertTrue(matcherS1S2.matches(s1));
        assertTrue(matcherS1S2.matches(s2));

        StopMatcher nullList = StopMatcher.parse(null);
        assertTrue(nullList.isEmpty());

        StopMatcher emptyList = StopMatcher.parse("");
        assertTrue(emptyList.isEmpty());

        StopMatcher degenerate = StopMatcher.parse(",,,");
        assertTrue(degenerate.isEmpty());

        boolean thrown = false;
        try {
            @SuppressWarnings("unused")
            StopMatcher badMatcher = StopMatcher.parse("A1");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);

    }

}

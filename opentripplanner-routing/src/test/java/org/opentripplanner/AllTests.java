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

package org.opentripplanner;

import org.opentripplanner.routing.TestHalfEdges;
import org.opentripplanner.routing.algorithm.TestAStar;
import org.opentripplanner.routing.algorithm.TestGraphPath;
import org.opentripplanner.routing.core.TestGraph;
import org.opentripplanner.routing.edgetype.TestStreet;
import org.opentripplanner.routing.edgetype.loader.TestPatternHopFactory;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for core");
        // $JUnit-BEGIN$
        suite.addTestSuite(TestAStar.class);
        suite.addTestSuite(TestGraph.class);
        suite.addTestSuite(TestGraphPath.class);
        suite.addTestSuite(TestPatternHopFactory.class);
        suite.addTestSuite(TestHalfEdges.class);
        suite.addTestSuite(TestStreet.class);
        // $JUnit-END$
        return suite;
    }

}

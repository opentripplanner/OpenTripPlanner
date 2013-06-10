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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TraverseResultTest {

    @Test
    public void testAddToExistingResultChain() {

        State resultChain = null;

        /* note: times are rounded to seconds toward zero */
        
        for (int i = 0; i < 4; i++) {
            State r = new State(null, i * 1000, new RoutingRequest());
            resultChain = r.addToExistingResultChain(resultChain);
        }

        assertEquals(3000, resultChain.getTimeSeconds(), 0.0);

        resultChain = resultChain.getNextResult();
        assertEquals(2000, resultChain.getTimeSeconds(), 0.0);

        resultChain = resultChain.getNextResult();
        assertEquals(1000, resultChain.getTimeSeconds(), 0.0);

        resultChain = resultChain.getNextResult();
        assertEquals(0000, resultChain.getTimeSeconds(), 0.0);

        resultChain = resultChain.getNextResult();
        assertNull(resultChain);
    }
}

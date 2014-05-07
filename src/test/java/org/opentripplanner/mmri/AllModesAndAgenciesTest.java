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

package org.opentripplanner.mmri;

import org.opentripplanner.api.model.Leg;

public class AllModesAndAgenciesTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/1a";
    }

    public void test1a1() {
        Leg[] legs = plan(+1388530800L, "1a1", "1a6", null, false, false, null, "", "", 5);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "1a2", "1a1", null);
        validateLeg(legs[2], 1388530980000L, 1388531040000L, "1a4", "1a3", null);
        validateLeg(legs[4], 1388531100000L, 1388531160000L, "1a6", "1a5", null);
    }
}

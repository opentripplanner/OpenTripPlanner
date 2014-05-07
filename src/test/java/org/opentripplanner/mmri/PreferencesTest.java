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

import static org.opentripplanner.routing.core.TraverseMode.BUS;

import org.opentripplanner.api.model.Leg;

public class PreferencesTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2c";
    }

    public void test2c1() {
        Leg[] legs = plan(+1388530860L, "2c1", "2c3", null, false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530860000L, 1388530920000L, "2c2", "2c1", null);
        validateLeg(legs[1], 1388530980000L, 1388531040000L, "2c3", "2c2", null);
    }

    public void test2c2() {
        Leg[] legs = plan(+1388530860L, "2c1", "2c3", null, false, false, BUS, "", "", 3);

        validateLeg(legs[1], 1388530920000L, 1388531160000L, "2c5", "2c4", null);
    }

    public void test2c3() {
        Leg[] legs = plan(+1388530860L, "2c1", "2c3", null, false, true, null, "", "", 3);

        validateLeg(legs[1], 1388530920000L, 1388531160000L, "2c5", "2c4", null);
    }
}

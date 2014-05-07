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

public class OnTripTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/2f";
    }

    public void test2f1() {
        Leg[] legs = plan(+1388530920L, null, "2f2", "2f|intercity", false, false, null, "", "", 2);

        validateLeg(legs[0], 1388530920000L, 1388531040000L, "2f3", null, null);
        validateLeg(legs[1], 1388531160000L, 1388531340000L, "2f2", "2f3", null);
    }
}

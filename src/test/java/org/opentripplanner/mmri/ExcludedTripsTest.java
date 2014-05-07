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

public class ExcludedTripsTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/3e";
    }

    public void test3e1() {
        Leg leg = plan(+1388530860L, "3e1", "3e2", null, false, false, null, "", "");

        validateLeg(leg, 1388530980000L, 1388531040000L, "3e2", "3e1", null);
    }
}

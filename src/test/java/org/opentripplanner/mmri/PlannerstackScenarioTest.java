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

public class PlannerstackScenarioTest extends MmriTest {
    @Override
    public final String getFeedName() {
        return "mmri/plannerstack_scenario";
    }

    public void testPlannerstackScenario() {
        Leg[] legs = plan(+1388531220L, null, "plannerstack_scenario2",
                "plannerstack_scenario|intercity", false, false, null, "", "", 2);

        validateLeg(legs[0], 1388531220000L, 1388531340000L, "plannerstack_scenario3", null, null);
        validateLeg(legs[1], 1388531400000L, 1388531640000L, "plannerstack_scenario2",
                "plannerstack_scenario3", null);
    }
}

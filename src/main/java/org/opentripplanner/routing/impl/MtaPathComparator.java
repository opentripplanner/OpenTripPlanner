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

package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.spt.GraphPath;

public class MtaPathComparator extends PathComparator {

    public MtaPathComparator(boolean compareStartTimes) {
        super(compareStartTimes);
    }

    // Walking trips should appear last in results
    @Override
    public int compare(GraphPath o1, GraphPath o2) {
        boolean o1NoTransit = o1.getTrips().isEmpty();
        boolean o2NoTransit = o2.getTrips().isEmpty();
        if (o1NoTransit && !o2NoTransit)
            return 1;
        if (!o1NoTransit && o2NoTransit)
            return -1;
        return super.compare(o1, o2);
    }

}

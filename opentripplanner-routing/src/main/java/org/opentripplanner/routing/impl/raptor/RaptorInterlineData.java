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

package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;

import org.onebusaway.gtfs.model.AgencyAndId;

public class RaptorInterlineData implements Serializable {
    private static final long serialVersionUID = -590861028792593164L;

    public RaptorRoute fromRoute;
    public RaptorRoute toRoute;
    public AgencyAndId fromTripId;
    public AgencyAndId toTripId;

    public int fromPatternIndex = -1;
    public int fromTripIndex = -1;
    public int toPatternIndex = -1;
    public int toTripIndex = -1;

}

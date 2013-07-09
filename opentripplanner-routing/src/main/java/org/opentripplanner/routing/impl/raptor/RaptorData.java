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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.T2;

public class RaptorData implements Serializable {

    private static final long serialVersionUID = -2387510738104439133L;

    public RaptorStop[] stops;

    public Collection<RaptorRoute> routes = new ArrayList<RaptorRoute>();

    public List<RaptorRoute>[] routesForStop;

    public HashMap<AgencyAndId, RaptorStop> raptorStopsForStopId = new HashMap<AgencyAndId, RaptorStop>();

    public RegionData regionData;

    //unused
    public List<T2<Double, RaptorStop>>[] nearbyStops;

    public MaxTransitRegions maxTransitRegions;

}

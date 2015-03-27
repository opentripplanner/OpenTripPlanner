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

package org.opentripplanner.routing.bike_rental;

import java.io.Serializable;
import java.util.Currency;
import java.util.List;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeBasedBikeRentalFareService implements FareService, Serializable {

    private static final long serialVersionUID = 5226621661906177942L;

    private static Logger log = LoggerFactory.getLogger(TimeBasedBikeRentalFareService.class);

    // Each entry is <max time, cents at that time>; the list is sorted in
    // ascending time order
    private List<P2<Integer>> pricing_by_second;

    private Currency currency;

    protected TimeBasedBikeRentalFareService(Currency currency, List<P2<Integer>> pricingBySecond) {
        this.currency = currency;
        this.pricing_by_second = pricingBySecond;
    }

    @Override
    public Fare getCost(GraphPath path) {
        int cost = 0;
        long start = -1;

        for (State state : path.states) {
            if (state.getVertex() instanceof BikeRentalStationVertex
                    && state.getBackState().getVertex() instanceof BikeRentalStationVertex) {
                if (start == -1) {
                    start = state.getTimeSeconds();
                } else {
                    int time_on_bike = (int) (state.getTimeSeconds() - start);
                    int ride_cost = -1;
                    for (P2<Integer> bracket : pricing_by_second) {
                        int time = bracket.first;
                        if (time_on_bike < time) {
                            ride_cost = bracket.second;
                            break;
                        }
                    }
                    if (ride_cost == -1) {
                        log.warn("Bike rental has no associated pricing (too long?) : "
                                + time_on_bike + " seconds");
                    } else {
                        cost += ride_cost;
                    }
                    start = -1;
                }
            }
        }

        Fare fare = new Fare();
        fare.addFare(FareType.regular, new WrappedCurrency(currency), cost);
        return fare;
    }
}

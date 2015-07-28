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

package org.opentripplanner.util;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Class which creates "Travel by" options list from supported transit modes and which extra modes are supported (bike sharing, bike & ride, park & ride)
 *
 * This list is then returned to the client which shows it in UI.
 * Created by mabu on 28.7.2015.
 */
public final class TravelOptionsMaker {

    private static List<TravelOption> staticTravelOptions;

    static {
        staticTravelOptions = new ArrayList<>(3);
        staticTravelOptions.add(new TravelOption(TraverseMode.WALK.toString()));
        staticTravelOptions.add(new TravelOption(TraverseMode.BICYCLE.toString()));
        staticTravelOptions.add(new TravelOption(TraverseMode.CAR.toString()));
    }

    public static List<TravelOption> makeOptions(Graph graph) {
        return makeOptions(graph.getTransitModes(), graph.hasBikeSharing, graph.hasBikeRide, graph.hasParkRide);
    }

    public static List<TravelOption> makeOptions(HashSet<TraverseMode> transitModes, boolean hasBikeSharing,
        boolean hasBikeRide, boolean hasParkRide) {
        List<TravelOption> travelOptions = new ArrayList<>(16);

        //Adds Transit, and all the transit modes
        if (!transitModes.isEmpty()) {
            travelOptions.add(new TravelOption(
                String.join(",", TraverseMode.TRANSIT.toString(), TraverseMode.WALK.toString()),
                TraverseMode.TRANSIT.toString()));

            for (TraverseMode transitMode: transitModes) {
                travelOptions.add(new TravelOption(
                    String.join(",", transitMode.toString(), TraverseMode.WALK.toString()),
                    transitMode.toString()));
            }
        }

        travelOptions.addAll(staticTravelOptions);

        if (hasBikeSharing) {
            travelOptions.add(new TravelOption(String.join(",", TraverseMode.WALK.toString(), "BICYCLE_RENT"), "BICYCLERENT"));
        }

        //If transit modes exists.
        if (!transitModes.isEmpty()) {
            //Adds bicycle transit mode
            travelOptions.add(new TravelOption(
                String.join(",", TraverseMode.TRANSIT.toString(), TraverseMode.BICYCLE.toString()),
                String.join("_", TraverseMode.TRANSIT.toString(), TraverseMode.BICYCLE.toString())));
            if (hasBikeSharing) {
                travelOptions.add(new TravelOption(String.join(",", TraverseMode.TRANSIT.toString(),
                    TraverseMode.WALK.toString(), "BICYCLE_RENT"), "TRANSIT_BICYCLERENT"));
            }
            if (hasParkRide) {
                travelOptions.add(new TravelOption(String.join(",", "CAR_PARK", TraverseMode.WALK.toString(), TraverseMode.TRANSIT.toString()),
                    "PARKRIDE"));
            }
            if (hasBikeRide) {
                travelOptions.add(new TravelOption(String.join(",", "BICYCLE_PARK", TraverseMode.WALK.toString(), TraverseMode.TRANSIT.toString()),
                    "BIKERIDE"));
            }
            travelOptions.add(new TravelOption(String.join(",", TraverseMode.CAR.toString(), TraverseMode.WALK.toString(), TraverseMode.TRANSIT.toString()),
                "KISSRIDE"));
        }



        return  travelOptions;
    }

}

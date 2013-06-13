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

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.OnBoardDepartPatternHop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.OnBoardDepartService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.OnboardDepartVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Graph service for depart-on-board mode.
 * 
 * Default implementation with initial position and date-time set.
 * 
 * We need departure date-time on top of initial position to determine the service day on which the
 * given trip is running.
 * 
 * Works only for standard trips: frequency-based is not (yet?) supported.
 * 
 * TODO The method is not optimal for looping trips, as several origin point can map to distinct
 * location on trip shape.
 * 
 * TODO Add mode with heuristics based on departure time only (may be too brittle?)
 * 
 * @author laurent
 */
public class OnBoardDepartServiceImpl implements OnBoardDepartService {
    private static final long serialVersionUID = -3729628250159575313L;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private static final Logger LOG = LoggerFactory.getLogger(OnBoardDepartServiceImpl.class);

    @Override
    public Vertex setupDepartOnBoard(RoutingContext ctx) {

        RoutingRequest opt = ctx.opt;

        /* 1. Get the list of PatternHop for the given trip ID. */
        AgencyAndId tripId = opt.getStartingTransitTripId();
        TransitIndexService transitIndexService = ctx.graph.getService(TransitIndexService.class);
        List<PatternHop> hops = transitIndexService.getPatternHopsForTrip(tripId);
        if (hops == null) {
            // TODO Shouldn't we bailout on a normal trip plan here, returning null ?
            throw new IllegalArgumentException("Unknown/invalid trip ID: " + tripId);
        }

        /*
         * 2. Get the best hop from the list, given the parameters. Currently look for nearest hop,
         * taking into account shape if available. If no shape are present, the computed hop and
         * fraction may be a bit away from what it should be.
         */
        Double lon = opt.getFrom().getLng(); // Origin point, optional
        Double lat = opt.getFrom().getLat();
        Coordinate point = lon == null || lat == null ? null : new Coordinate(lon, lat);
        if (point == null) {
            // If you want to support this, you have to give in the request the hop index and hop
            // fraction percentage.
            throw new UnsupportedOperationException(
                    "Unsupported on-board depart without initial location.");
        }
        PatternHop bestHop = null;
        double minDist = Double.MAX_VALUE;
        for (PatternHop hop : hops) {
            LineString line = hop.getGeometry();
            double dist = distanceLibrary.fastDistance(point, line);
            if (dist < minDist) {
                minDist = dist;
                bestHop = hop;
            }
        }
        if (minDist > 1000)
            LOG.warn(
                    "On-board depart: origin point suspiciously away from nearest trip shape ({} meters)",
                    minDist);
        else
            LOG.info("On-board depart: origin point {} meters away from hop shape", minDist);

        /*
         * 3. Compute the fraction covered percentage of the current hop. This assume a constant
         * trip speed alongside the whole hop: this should be quite precise for small hops (buses),
         * a bit less for longer ones (long distance train). Shape linear distance is of no help
         * here, as the unit is arbitrary (and probably usually a distance).
         */
        P2<LineString> geomPair = GeometryUtils.splitGeometryAtPoint(bestHop.getGeometry(), point);
        LineString geomRemaining = geomPair.getSecond();
        double total = bestHop.getGeometry().getLength();
        double remaining = geomRemaining.getLength();
        float fractionCovered = total > 0.0 ? (float) (remaining / total) : 0.0f;

        PatternStopVertex nextStop = (PatternStopVertex) bestHop.getToVertex();
        int stopIndex = bestHop.getStopIndex();

        /* 3. Get the tripTimes */
        TableTripPattern pattern = nextStop.getTripPattern();
        TripTimes tripTimes = null;
        if (ctx.timetableSnapshot == null) {
            tripTimes = pattern.getTripTimes(pattern.getTripIndex(tripId));
        } else {
            Timetable timeTable = ctx.timetableSnapshot.resolve(pattern);
            tripTimes = timeTable.getTripTimes(timeTable.getTripIndex(tripId));
        }

        /*
         * 4. Compute service day based on given departure day/time relative to scheduled/real-time
         * trip time for hop. This is needed as for some trips any service day can apply.
         */
        int depTime = tripTimes.getDepartureTime(stopIndex);
        int arrTime = tripTimes.getArrivalTime(stopIndex);
        int estTime = Math.round(depTime * fractionCovered + arrTime * (1 - fractionCovered));
        ServiceDay bestServiceDay = null;
        int minDelta = Integer.MAX_VALUE;
        int actDelta = 0;
        for (ServiceDay serviceDay : ctx.serviceDays) {
            int time = serviceDay.secondsSinceMidnight(opt.dateTime);
            /*
             * TODO Weight differently early vs late time, as the probability of any transit being
             * late is higher than being early. However, this has impact if your bus is more than
             * 12h late, I don't think this would happen really often.
             */
            int deltaTime = Math.abs(time - estTime);
            if (deltaTime < minDelta) {
                minDelta = deltaTime;
                actDelta = time - estTime;
                bestServiceDay = serviceDay;
            }
        }
        if (minDelta > 60000)
            // Being more than 1h late should not happen often
            LOG.warn(
                    "On-board depart: delta between scheduled/real-time and actual time suspiciously large: {} seconds.",
                    actDelta);
        else
            LOG.info(
                    "On-board depart: delta between scheduled/real-time and actual time is {} seconds.",
                    actDelta);
        OnboardDepartVertex onboardDepart = new OnboardDepartVertex("on_board_depart", lon, lat);
        OnBoardDepartPatternHop startHop = new OnBoardDepartPatternHop(onboardDepart, nextStop,
                tripTimes, bestServiceDay, stopIndex, fractionCovered);
        startHop.setGeometry(geomRemaining);
        return onboardDepart;
    }
}

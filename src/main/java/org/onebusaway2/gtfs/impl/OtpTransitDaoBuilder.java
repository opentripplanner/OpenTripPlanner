/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation; either version 3 of
 the License; or (at your option) any later version.

 This program is distributed in the hope that it will be useful;
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not; see <http://www.gnu.org/licenses/>. 
*/
package org.onebusaway2.gtfs.impl;

import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.FareAttribute;
import org.onebusaway2.gtfs.model.FareRule;
import org.onebusaway2.gtfs.model.FeedInfo;
import org.onebusaway2.gtfs.model.Frequency;
import org.onebusaway2.gtfs.model.IdentityBean;
import org.onebusaway2.gtfs.model.Pathway;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.ShapePoint;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.Transfer;
import org.onebusaway2.gtfs.model.Trip;
import org.opentripplanner.model.OtpTransitDao;

import java.util.ArrayList;
import java.util.List;

public class OtpTransitDaoBuilder {
    private final List<Agency> agencies = new ArrayList<>();

    private final List<ServiceCalendarDate> calendarDates = new ArrayList<>();

    private final List<ServiceCalendar> calendars = new ArrayList<>();

    private final List<FareAttribute> fareAttributes = new ArrayList<>();

    private final List<FareRule> fareRules = new ArrayList<>();

    private final List<FeedInfo> feedInfos = new ArrayList<>();

    private final List<Frequency> frequencies = new ArrayList<>();

    private final List<Pathway> pathways = new ArrayList<>();

    private final List<Route> routes = new ArrayList<>();

    private final List<ShapePoint> shapePoints = new ArrayList<>();

    private final List<Stop> stops = new ArrayList<>();

    private final List<StopTime> stopTimes = new ArrayList<>();

    private final List<Transfer> transfers = new ArrayList<>();

    private final List<Trip> trips = new ArrayList<>();

    public OtpTransitDaoBuilder() {
    }

    public OtpTransitDaoBuilder(OtpTransitDao dao) {
        add(dao);
    }

    public OtpTransitDaoBuilder add(OtpTransitDao other) {
        agencies.addAll(other.getAllAgencies());
        calendarDates.addAll(other.getAllCalendarDates());
        calendars.addAll(other.getAllCalendars());
        fareAttributes.addAll(other.getAllFareAttributes());
        fareRules.addAll(other.getAllFareRules());
        feedInfos.addAll(other.getAllFeedInfos());
        frequencies.addAll(other.getAllFrequencies());
        pathways.addAll(other.getAllPathways());
        routes.addAll(other.getAllRoutes());
        shapePoints.addAll(other.getAllShapePoints());
        stops.addAll(other.getAllStops());
        stopTimes.addAll(other.getAllStopTimes());
        transfers.addAll(other.getAllTransfers());
        trips.addAll(other.getAllTrips());
        return this;
    }

    public List<Agency> getAgencies() {
        return agencies;
    }

    public List<ServiceCalendarDate> getCalendarDates() {
        return calendarDates;
    }

    public List<ServiceCalendar> getCalendars() {
        return calendars;
    }

    public List<FareAttribute> getFareAttributes() {
        return fareAttributes;
    }

    public List<FareRule> getFareRules() {
        return fareRules;
    }

    public List<FeedInfo> getFeedInfos() {
        return feedInfos;
    }

    public List<Frequency> getFrequencies() {
        return frequencies;
    }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<ShapePoint> getShapePoints() {
        return shapePoints;
    }

    public List<Stop> getStops() {
        return stops;
    }

    public List<StopTime> getStopTimes() {
        return stopTimes;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public OtpTransitDao build() {

        createNoneExistingIds();

        return new OtpTransitDaoImpl(agencies, calendarDates, calendars, fareAttributes, fareRules,
                feedInfos, frequencies, pathways, routes, shapePoints, stops, stopTimes, transfers,
                trips);
    }

    private void createNoneExistingIds() {
        generateNoneExistingIds(calendarDates);
        generateNoneExistingIds(calendars);
        generateNoneExistingIds(fareRules);
        generateNoneExistingIds(feedInfos);
        generateNoneExistingIds(frequencies);
        generateNoneExistingIds(stopTimes);
        generateNoneExistingIds(transfers);
    }

    static <T extends IdentityBean<Integer>> void generateNoneExistingIds(List<T> entities) {
        int maxId = 0;
        for (T it : entities) {
            maxId = zeroOrNull(it.getId()) ? maxId : Math.max(maxId, it.getId());
        }
        for (T it : entities) {
            if(zeroOrNull(it.getId())) {
                it.setId(++maxId);
            }
        }
    }

    private static boolean zeroOrNull(Integer id) {
        return id == null || id == 0;
    }
}

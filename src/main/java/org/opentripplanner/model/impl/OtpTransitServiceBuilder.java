package org.opentripplanner.model.impl;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FlexArea;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.IdentityBean;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.OtpTransitService;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for building a {@link OtpTransitService}. The instance returned by the
 * {@link #build()} method is read only, and this class provide a mutable collections to construct
 * a {@link OtpTransitService} instance.
 */
public class OtpTransitServiceBuilder {
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

    private final List<FlexArea> flexAreas = new ArrayList<>();

    public OtpTransitServiceBuilder() {
    }

    public OtpTransitServiceBuilder(OtpTransitService transitService) {
        add(transitService);
    }

    public OtpTransitServiceBuilder add(OtpTransitService other) {
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
        flexAreas.addAll(other.getAllAreas());
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

    public List<FlexArea> getFlexAreas() {
        return flexAreas;
    }

    public OtpTransitService build() {

        createNoneExistentIds();

        return new OtpTransitServiceImpl(agencies, calendarDates, calendars, fareAttributes, fareRules,
                feedInfos, frequencies, pathways, routes, shapePoints, stops, stopTimes, transfers,
                trips, flexAreas);
    }

    private void createNoneExistentIds() {
        generateNoneExistentIds(feedInfos);
    }

    static <T extends IdentityBean<String>> void generateNoneExistentIds(List<T> entities) {
        int maxId = 0;


        for (T it : entities) {
            try {
                if(it.getId() != null) {
                    maxId = Math.max(maxId, Integer.parseInt(it.getId()));
                }
            } catch (NumberFormatException ignore) {}
        }

        for (T it : entities) {
            try {
                if(it.getId() == null || Integer.parseInt(it.getId()) == 0) {
                    it.setId(Integer.toString(++maxId));
                }
            }
            catch (NumberFormatException ignore) { }
        }
    }
}

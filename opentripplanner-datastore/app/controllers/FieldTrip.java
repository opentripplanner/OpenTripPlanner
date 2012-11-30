package controllers;

import play.*;
import play.mvc.*;

import java.text.DateFormatSymbols;
import java.util.*;

import models.*;



public class FieldTrip extends Application {

    @Before(unless={"newTrip","addTripFeedback"}, priority=1)
    public static void checkLogin () {
        User user = getUser();
        if (!user.canScheduleFieldTrips()) {
            forbidden();
        }
    }

    public static void index() {
        //index at present does nothing
        render();
    }

    /**
     * y/m/d are the day for which we would like a calendar.
     */
    public static void getCalendar(int year, int month, int day) {
        List<ScheduledFieldTrip> fieldTrips;

        fieldTrips = ScheduledFieldTrip.find("year(serviceDay) = ? and month(serviceDay) = ? and day(serviceDay) = ? " +
                                              "order by departure", 
                                              year, month, day).fetch();

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        String monthName = months[month - 1];
        render(fieldTrips, year, month, monthName, day);
    }

    public static void getFieldTrip(int id) {
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(id);
        renderJSON(fieldTrip);
    }

    public static void newTrip(ScheduledFieldTrip trip) {
        //TODO: is setting id to null the right way to ensure that an
        //existing trip is not overwritten?
        trip.id = null;
        User user = getUser();
        if (!user.canScheduleFieldTrips()) {
            //TODO: is this safe if those itineraries exist?
            trip.groupItineraries.clear();
        }
        trip.save();
        Long id = trip.id;
        renderJSON(id);
    }

    public static void addTripFeedback(FieldTripFeedback feedback) {
        feedback.id = null;
        feedback.save();
        render();
    }

    public static void addItinerary(long fieldTripId, GroupItinerary itinerary) {
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(fieldTripId);
        itinerary.fieldTrip = fieldTrip;
        fieldTrip.groupItineraries.add(itinerary);
        itinerary.save();
        Long id = itinerary.id;
        renderJSON(id);
    }

    public static void deleteTrip(Long id) {
        ScheduledFieldTrip trip = ScheduledFieldTrip.findById(id);
        trip.delete();
        render(id);
    }

}
package controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import play.*;
import play.mvc.*;

import java.text.DateFormatSymbols;
import java.util.*;

import models.*;
import play.data.binding.As;



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

    public static void getFieldTrip(long id) {
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(id);
        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(fieldTrip));
    }
    
    public static void getFieldTrips(@As("MM/dd/yyyy") Date date, Integer limit) {
        System.out.println("getFTs, date="+date);
        List<ScheduledFieldTrip> trips;
        String sql = "";
        if(date != null) {          
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            sql = "year(serviceDay) = " + cal.get(Calendar.YEAR) + 
                  " and month(serviceDay) = " + (cal.get(Calendar.MONTH)+1) + 
                  " and day(serviceDay) = "+cal.get(Calendar.DAY_OF_MONTH)+" ";
        }
        sql += "order by departure";
        if(limit == null)
            trips = ScheduledFieldTrip.find(sql).fetch();
        else {
            trips = ScheduledFieldTrip.find(sql).fetch(limit);
        }

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(trips));
        //renderJSON(trips);
    }
    
    public static void getGTFSTripsInUse(@As("MM/dd/yyyy") Date date, Integer limit) {
        System.out.println("getFTs, date="+date);
        List<ScheduledFieldTrip> trips;
        String sql = "";
        if(date != null) {          
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            sql = "year(serviceDay) = " + cal.get(Calendar.YEAR) + 
                  " and month(serviceDay) = " + (cal.get(Calendar.MONTH)+1) + 
                  " and day(serviceDay) = "+cal.get(Calendar.DAY_OF_MONTH)+" ";
        }
        sql += "order by departure";
        if(limit == null)
            trips = ScheduledFieldTrip.find(sql).fetch();
        else {
            trips = ScheduledFieldTrip.find(sql).fetch(limit);
        }

        Set<GTFSTrip> gtfsTrips = new HashSet<GTFSTrip>();
        for(ScheduledFieldTrip fieldTrip : trips) {
            for(GroupItinerary itin : fieldTrip.groupItineraries) {
                for(GTFSTrip gtfsTrip : itin.trips) {
                    gtfsTrips.add(gtfsTrip);
                }
            }
        }
        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(gtfsTrips));
    }

    
    public static void newTrip(ScheduledFieldTrip trip) {
        //TODO: is setting id to null the right way to ensure that an
        //existing trip is not overwritten?
        trip.id = null;
        trip.serviceDay = trip.departure;
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

    public static void addItinerary(long fieldTripId, GroupItinerary itinerary, GTFSTrip[] trips) {
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(fieldTripId);
        itinerary.fieldTrip = fieldTrip;
        fieldTrip.groupItineraries.add(itinerary);
        itinerary.save();
        Long id = itinerary.id;
        //GroupItinerary itin2 = GroupItinerary.findById(id);
        
        itinerary.trips = new ArrayList<GTFSTrip>();
        for(GTFSTrip gtrip : trips) {
          gtrip.groupItinerary = itinerary;
          itinerary.trips.add(gtrip);
          gtrip.save();
        }
        renderJSON(id);
    }

    public static void deleteTrip(Long id) {
        ScheduledFieldTrip trip = ScheduledFieldTrip.findById(id);
        trip.delete();
        renderJSON(id);
    }

    public static void newRequest(FieldTripRequest request) {
        request.id = null;
        request.save();
        Long id = request.id;
        renderJSON(id);
    }
    
    public static void getRequests(Integer limit) {
        List<FieldTripRequest> requests;
        String sql = "";
        if(limit == null)
            requests = FieldTripRequest.find(sql).fetch();
        else {
            requests = FieldTripRequest.find(sql).fetch(limit);
        }

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(requests));
    }
}
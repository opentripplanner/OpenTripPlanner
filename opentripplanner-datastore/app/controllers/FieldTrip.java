package controllers;

import models.fieldtrip.ScheduledFieldTrip;
import models.fieldtrip.FieldTripFeedback;
import models.fieldtrip.FieldTripRequest;
import models.fieldtrip.GroupItinerary;
import models.fieldtrip.GTFSTrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import static controllers.Application.checkLogin;
import static controllers.Calltaker.checkAccess;
import play.*;
import play.mvc.*;

import java.text.DateFormatSymbols;
import java.util.*;

import models.*;
import play.data.binding.As;



public class FieldTrip extends Application {
    
    @Util
    public static void checkAccess(TrinetUser user) {
        if(user == null) {
            System.out.println("null user in FieldTrip module");
            forbidden("null user");
        }
        if(!user.hasFieldTripAccess()) {
            System.out.println("User " + user.username + " has insufficient access for FieldTrip module");
            forbidden("insufficient access privileges");
        }
    }
    
    /*@Before(unless={"newTrip","addTripFeedback","getCalendar","newRequest","newRequestForm"}, priority=1)
    public static void checkLogin () {
        String username = params.get("userName");
        String password = params.get("password");
        
        System.out.println("checkLogin "+username);
        User user = checkUser(username, password);
        //User user = getUser();
        if (user == null || !user.canScheduleFieldTrips()) {
            forbidden();
        }
        System.out.println("checkLogin success");
    }*/

    public static void index() {
        //index at present does nothing
        render();
    }

    /**
     * y/m/d are the day for which we would like a calendar.
     */
    public static void getCalendar(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        if(year == 0) year = cal.get(Calendar.YEAR);
        if(month == 0) month = cal.get(Calendar.MONTH)+1;
        if(day == 0) day = cal.get(Calendar.DAY_OF_MONTH);

        List<ScheduledFieldTrip> fieldTrips;

        fieldTrips = ScheduledFieldTrip.find("year(serviceDay) = ? and month(serviceDay) = ? and day(serviceDay) = ? " +
                                              "order by departure", 
                                              year, month, day).fetch();

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        String monthName = months[month - 1];
        render(fieldTrips, year, month, monthName, day);
    }

    public static void opsReport(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        if(year == 0) year = cal.get(Calendar.YEAR);
        if(month == 0) month = cal.get(Calendar.MONTH)+1;
        if(day == 0) day = cal.get(Calendar.DAY_OF_MONTH);

        List<GTFSTrip> gtfsTrips = GTFSTrip.find("year(groupItinerary.fieldTrip.serviceDay) = ? and month(groupItinerary.fieldTrip.serviceDay) = ? and day(groupItinerary.fieldTrip.serviceDay) = ? " +
                                              "order by depart", 
                                              year, month, day).fetch();

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        String monthName = months[month - 1];
        render(gtfsTrips, year, month, monthName, day);
    }
    
    
    public static void getFieldTrip(long id) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(id);
        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .create();
        renderJSON(gson.toJson(fieldTrip));
    }
    
    public static void getFieldTrips(@As("MM/dd/yyyy") Date date, Integer limit) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
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
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
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

    
    public static void newTrip(long requestId, ScheduledFieldTrip trip) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        FieldTripRequest ftRequest = FieldTripRequest.findById(requestId);

        //TODO: is setting id to null the right way to ensure that an
        //existing trip is not overwritten?
        trip.id = null;
        trip.request = ftRequest;
        trip.serviceDay = trip.departure;
        trip.createdBy = user.username;
        
        trip.save();
        
        ftRequest.trips.add(trip);
        ftRequest.save();
        
        System.out.println("saved ScheduledFieldTrip, id="+trip.id);
        Long id = trip.id;
        renderJSON(id);
    }
    
    public static void addItinerary(long fieldTripId, GroupItinerary itinerary, GTFSTrip[] trips) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        System.out.println("aI / fieldTripId="+fieldTripId);
        ScheduledFieldTrip fieldTrip = ScheduledFieldTrip.findById(fieldTripId);
        //System.out.println("aI / fieldTrip="+fieldTrip);
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
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        ScheduledFieldTrip trip = ScheduledFieldTrip.findById(id);
        trip.delete();
        renderJSON(id);
    }
    
    /* FieldTripRequest methods */
    
    public static void newRequestForm() {
        render();
    }
    
    public static void newRequest(FieldTripRequest req) {
        System.out.println("newRequest tn="+req.teacherName);
        if(req.teacherName != null) {
            req.id = null;
            req.save();
            Long id = req.id;
            System.out.println("about to render");
            render(req);
        }
        else {
            badRequest();
        }
    }
    
    public static void getRequests(Integer limit) {
        TrinetUser user = checkLogin();        
        checkAccess(user);
      
        List<FieldTripRequest> requests;
        String sql = "order by timeStamp desc";
        if(limit == null)
            requests = FieldTripRequest.find(sql).fetch();
        else {
            requests = FieldTripRequest.find(sql).fetch(limit);
        }

        Gson gson = new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()  
          .serializeNulls()
          .create();
        renderJSON(gson.toJson(requests));
    }
    
    public static void setRequestStatus(long requestId, String status) {
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            req.status = status;
            req.save();
            if(status.equals("cancelled")) {
                for(ScheduledFieldTrip trip : req.trips) {
                    trip.delete();
                }
            }
            renderJSON(requestId);
        }
        else {
            badRequest();
        }        
    }

    public static void setRequestClasspassId(long requestId, String classpassId) {
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            if(classpassId != null && classpassId.length() == 0) classpassId = null;
            req.classpassId = classpassId;
            req.save();
            renderJSON(requestId);
        }
        else {
            badRequest();
        }        
    }
    
    /* FieldTripFeedback */
    
    public static void feedbackForm(long requestId) {
        System.out.println("ff req="+requestId);
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            render(req);
        }
        else {
            badRequest();
        }
    }
    
    public static void addFeedback(FieldTripFeedback feedback, long requestId) {
        System.out.println("addFeedback reqId="+requestId);
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            feedback.id = null;
            feedback.request = req;
            feedback.save();
            
            req.feedback.add(feedback);
            req.save();
            
            render(feedback);
        }
        else {
            badRequest();
        }
    }

    /* Receipt Generation */
    
    public static void receipt(long requestId) {
        FieldTripRequest req = FieldTripRequest.findById(requestId);
        if(req != null) {
            render(req);
        }
    }
    
}
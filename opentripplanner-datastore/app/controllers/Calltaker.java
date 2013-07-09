package controllers;

import models.calltaker.Call;

import java.util.*;

import models.calltaker.TripQuery;
import play.data.binding.As;

public class Calltaker extends Application {

    public static void index() {
        //List<OTPQuery> queries = OTPQuery.all().fetch(10);
        //render(queries);
        //List<Call> calls = Call.all().fetch(10);
        List<TripQuery> items = TripQuery.all().fetch(10);
        render(items);

    }
    
    public static void options() {        
    }
    
    /*public static void newCall(String userName, 
            @As("yyyy-MM-dd'T'HH:mm:ss") Date startTime,
            @As("yyyy-MM-dd'T'HH:mm:ss") Date endTime) {
        System.out.println("newCall startTime="+startTime);
        Call c = new Call(userName, startTime, endTime);
        c.save();
        renderJSON(c.id);
    }*/
    
    public static void newCall(Call call) {
        call.userName = params.get("userName");
        call.save();
        renderJSON(call.id);
    }
    

    public static void getCall(String userName, Integer limit) {
        System.out.println("getCall");
        List<Call> calls;
        if(limit == null)
            calls = Call.find("userName = '"+userName+"' order by startTime").fetch();
        else {
            calls = Call.find("userName = '"+userName+"' order by startTime").fetch(limit);
            System.out.println("fetched w/ limit = "+limit);
        }

        renderJSON(calls);
    }

    public static void getQuery(Call call, Integer limit) {
        List<TripQuery> queries;
        if(limit == null)
            queries = TripQuery.find("call.id = '"+call.id+"' order by timeStamp").fetch();
        else {
            queries = TripQuery.find("call.id = '"+call.id+"' order by timeStamp").fetch(limit);
        }
        renderJSON(queries);
    }
    
    public static void newQuery(TripQuery query) {
        System.out.println("nQ request params: " + request.params.allSimple());
        query.userName = params.get("userName");
        query.save();
        renderJSON(query.id);
    }

    public static void deleteQuery(Long id) {
        TripQuery query = TripQuery.findById(id);  
        query.delete();
        render(id);
    }  
}
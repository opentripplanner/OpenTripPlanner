package controllers;

import static controllers.Application.checkLogin;
import models.calltaker.Call;

import java.util.*;
import models.TrinetUser;

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
        TrinetUser user = checkLogin();        
        call.user = user;
        call.save();
        System.out.println("saved call for "+call.user);
        renderJSON(call.id);
    }
    

    public static void getCall(Integer limit) {
        TrinetUser user = checkLogin();        
        System.out.println("getCall for "+user);
        List<Call> calls;
        if(limit == null)
            calls = Call.find("byUser order by startTime", user).fetch();
        else {
            //calls = Call.find("userName = '"+userName+"' order by startTime").fetch(limit);
            calls = Call.find("byUser", user).fetch(limit);
            System.out.println("fetched w/ limit = "+limit);
        }

        renderJSON(calls);
    }

    public static void getQuery(Call call, Integer limit) {
        TrinetUser user = checkLogin();        
        List<TripQuery> queries;
        if(limit == null)
            queries = TripQuery.find("call.id = '"+call.id+"' order by timeStamp").fetch();
        else {
            queries = TripQuery.find("call.id = '"+call.id+"' order by timeStamp").fetch(limit);
        }
        renderJSON(queries);
    }
    
    public static void newQuery(TripQuery query) {
        TrinetUser user = checkLogin();        
        //System.out.println("nQ request params: " + request.params.allSimple());
        //query.user = user;
        query.save();
        renderJSON(query.id);
    }

    public static void deleteQuery(Long id) {
        TrinetUser user = checkLogin();        
        TripQuery query = TripQuery.findById(id);  
        query.delete();
        render(id);
    }  
}
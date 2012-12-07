package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Calltaker extends Application {
    
  @Before
    public static void setCORS()  {
        Http.Header hd = new Http.Header();
        hd.name = "Access-Control-Allow-Origin";
        hd.values = new ArrayList<String>();
        hd.values.add("*");
        Http.Response.current().headers.put("Access-Control-Allow-Origin",hd);      
    }
    
    public static void index() {
        List<OTPQuery> queries = OTPQuery.all().fetch(10);
        render(queries);
    }
    
    public static void getQueries(String userName, Integer limit) {
        List<OTPQuery> queries;
        if(limit == null)
            queries = OTPQuery.find("userName", userName).fetch();
        else {
            queries = OTPQuery.find("userName = '"+userName+"' order by timeStamp desc").fetch(limit);
            System.out.println("fetched w/ limit = "+limit);
        }
        renderJSON(queries);
    }
    
    public static void newQuery(String userName, String queryParams, String fromPlace, String toPlace) {
        OTPQuery query = new OTPQuery(userName, queryParams, fromPlace, toPlace);
        query.save();
        Long id = query.id;
        render(userName, id);
    }

    public static void deleteQuery(Long id) {
        OTPQuery query = OTPQuery.findById(id);  
        query.delete();
        render(id);
    }  
}
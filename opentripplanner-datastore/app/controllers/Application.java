package controllers;

import com.google.gson.Gson;
import play.*;
import play.cache.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    @Before
    public static void setCORS()  {
        Http.Header origin = new Http.Header();
        origin.name = "Access-Control-Allow-Origin";
        origin.values = new ArrayList<String>();
        origin.values.add("*");
        Http.Response.current().headers.put("Access-Control-Allow-Origin",origin);      
        
        Http.Header headers = new Http.Header();
        headers.name = "Access-Control-Allow-Headers";
        headers.values = new ArrayList<String>();
        headers.values.add("Origin, X-Requested-With, Content-Type, Accept");
                
        //headers.values.add("Origin");
        //headers.values.add("X-Requested-With");
        //headers.values.add("Accept");
       
        Http.Response.current().headers.put("Access-Control-Allow-Headers",headers);      

    }
    
    /*@Util
    public static void renderJSON(Object obj) {
        if (request.params._contains("callback")) {
            Gson gson = new Gson();
            String json = gson.toJson(obj);
            //System.out.println("returning as jsonp (u): "+json);
            renderText(request.params.get("callback") + "(" + json + ")");            
        } else {
            renderJSON(obj);
        }      
    }*/
    
    @Before(priority=0)
    public static void checkPassword() {
        request.user = null;

        String username = params.get("userName");
        String password = params.get("password");
        User user = getUser(username);
        if (user == null) {
            Logger.debug("no user by this username: %s (count = %s)", username, User.count());
            forbidden();
        }
        if (user.checkPassword(password)) {
            request.user = username;
            Logger.debug("Logged in %s", user.userName);
        } else {
            Logger.debug("bad password");
            forbidden();
        }
    }

    static User getUser(String username) {
        User user = Cache.get(username, User.class);
        if (user == null) {
            Logger.debug("no user in cache");
            user = User.find("byUsername", username).first();
            Cache.set(username, user);
        }
        return user;
    }

    static User getUser() {
        return getUser(request.user);
    }
}
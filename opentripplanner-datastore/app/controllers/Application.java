package controllers;

import java.math.BigInteger;
import java.security.SecureRandom;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    private static SecureRandom random = new SecureRandom();
  
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
    
    
    // called internally by CallTaker/FieldTrip controller
    public static TrinetUser checkLogin() {
        String sessionId = params.get("sessionId");
        
        Session userSession = Session.find("bySessionId", sessionId).first();
        if(userSession == null) {
            forbidden();
        }
        System.out.println("retrieved session for user: "+userSession.user);
        return userSession.user;
    }
    
    public static void newSession() {
        Map<String, String> resp = new HashMap<String, String>();
        resp.put("sessionId", nextSessionId());
        renderJSON(resp);
    }

    public static void checkSession(String sessionId) {
        Session userSession = Session.find("bySessionId", sessionId).first();

        Map<String, String> resp = new HashMap<String, String>();
        resp.put("sessionId", sessionId);
        if(session != null) {
            resp.put("username", userSession.user.username);            
        }
        renderJSON(resp);
    }
    
    public static void verifyLogin(String session, String redirect) {

        System.out.println("\n** verifyLogin ** " + redirect +  " \n");
        System.out.println("headers: "+ request.headers);
        
        String username = request.headers.get("x-remote-user").value();
        TrinetUser user = TrinetUser.find("byUsername", username).first();
        Session userSession = new Session(session, user);
        userSession.save();
        System.out.println("initialized session " + session + " for user "+username);
        
        String redirectUrl = redirect + "?sessionId=" + session;
        System.out.println("redirecting to: " + redirectUrl);
        redirect(redirectUrl);
    }
        
    public static String nextSessionId() {
        return new BigInteger(130, random).toString(32);
    }
    
}
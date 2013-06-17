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
    
    public static void initLogin() {
        String username = "test";
        TrinetUser user = TrinetUser.find("byUsername", username).first();
        String sessionId = null;
        if(user != null) {
            sessionId = nextSessionId();
            Session trinetSession = new Session(sessionId, user);
            trinetSession.save();
            System.out.println("saved session" + sessionId + " for user "+user);
        }
        
        Map<String, String> resp = new HashMap<String, String>();
        resp.put("sessionId", sessionId);
        resp.put("username", user.username);
        renderJSON(resp);
    }
    
    public static TrinetUser checkLogin() {
        String sessionId = params.get("sessionId");
        
        Session session = Session.find("bySessionId", sessionId).first();
        if(session == null) {
            forbidden();
        }
        System.out.println("retrieved session for user: "+session.user);
        return session.user;
    }
    
        
    public static String nextSessionId() {
        return new BigInteger(130, random).toString(32);
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
    
    
    /*@Before(priority=0)
    public static void checkPassword() {
        request.user = null;

        String username = params.get("userName");
        String password = params.get("password");
        System.out.println("checkPassword "+username);
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
    }*/
    

    /*static User getUser() {
        System.out.println("getUser(): "+request.user);
        return getUser(request.user);
    }*/
}
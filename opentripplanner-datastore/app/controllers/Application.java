package controllers;

import play.*;
import play.cache.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    @Before
    public static void setCORS()  {
        Http.Header hd = new Http.Header();
        hd.name = "Access-Control-Allow-Origin";
        hd.values = new ArrayList<String>();
        hd.values.add("*");
        Http.Response.current().headers.put("Access-Control-Allow-Origin",hd);      
    }

    @Before(priority=0)
    public static void checkPassword() {
        request.user = null;

        String username = params.get("userName");
        String password = params.get("password");
        User user = Cache.get(username, User.class);
        if (user == null) {
            Logger.debug("no user in cache");
            user = User.find("byUsername", username).first();
            if (user == null) {
                Logger.debug("no user by this username: count = %s", User.count());
                forbidden();
            }
        }
        if (user.checkPassword(password)) {
            request.user = username;
            Logger.debug("Logged in %s", user.userName);
        } else {
            Logger.debug("bad password");
            forbidden();
        }
    }

    static User getUser() {
        return User.find("byUsername", request.user).first();
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import play.*;
import play.cache.*;
import play.mvc.*;

import java.util.*;

import models.*;

/**
 *
 * @author demory
 */

@With(Secure.class)
public class Admin extends Controller {
  
    public static void getUsers() {
        List<TrinetUser> users = TrinetUser.findAll();
        render(users);
    }

    public static void addUser(TrinetUser user) {
        user.save();
        getUsers();
    }
    
    public static void deleteUser(long id) {
        TrinetUser user = TrinetUser.findById(id);
       
        List<Session> sessions;

        sessions = Session.find("byUser", user).fetch();
        for(Session sess : sessions) {
            sess.delete();
        }
       
        user.delete();
        getUsers();
    }

    public static void changeUserRole(long id, String role) {
        TrinetUser user = TrinetUser.findById(id);
        user.role = role;
        user.save();       
        getUsers();
    }
    
}

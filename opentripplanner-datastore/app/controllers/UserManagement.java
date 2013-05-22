package controllers;

import play.*;
import play.cache.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class UserManagement extends Application {
    @Before(unless="updateSelf")
    public static void checkLogin () {
        User user = getUser();
        if (!user.isAdmin()) {
            forbidden();
        }
    }

    /**
     */
    public static void updateSelf(User newUser) {
        User user = getUser();
        if (newUser.id != user.id || !newUser.userName.equals(user.userName)) {
            forbidden();
            return;
        }
        newUser.role = user.role; //can't change own role
        newUser.save();
    }

    public static void getUsers() {
        List<User> users = User.findAll();
        renderJSON(users);
    }

   public static void deleteUser(int id) {
       User user = User.findById(id);
       user.delete();
    }

    /**
       Note that this also allows updating users
     */
    public static void addUser(User user) {
        user.save();
    }

}
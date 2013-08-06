package controllers;
 
import models.Administrator;

public class Security extends Secure.Security {
    
    static boolean authenticate(String username, String password) {
        Administrator admin = Administrator.find("byUsername", username).first();
        return admin != null && admin.password.equals(password);
    }
}
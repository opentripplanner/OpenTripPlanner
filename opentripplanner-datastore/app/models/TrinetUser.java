/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package models;

import javax.persistence.Entity;
import play.db.jpa.*;

/**
 *
 * @author demory
 */

@Entity
public class TrinetUser extends Model {
  
    public String username;
    
    public String role;
    
    @Override
    public String toString() {
        return String.format("TrinetUser %s (%s)", username, role);
    }
    
    public boolean hasCalltakerAccess() {
        return role.equals("calltaker") || role.equals("all");
    }

    public boolean hasFieldTripAccess() {
        return role.equals("fieldtrip") || role.equals("all");
    }

}

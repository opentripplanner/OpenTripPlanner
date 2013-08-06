
package models;

import javax.persistence.*;
 
import play.db.jpa.*;

@Entity
public class Administrator extends Model {

    public String username;

    public String password;
}
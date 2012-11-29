package models;

import com.lambdaworks.crypto.SCryptUtil;

import java.util.Date;
import java.util.List;
import javax.persistence.*;
 
import play.data.binding.*;
import play.db.jpa.*;

@Entity
public class User extends Model {

    public String userName;

    /** the password, encrypted with scrypt */
    private String cryptedPassword;

    @Column(nullable=false, name="cryptedPassword")
    public void setPassword(String password) {
        cryptedPassword = SCryptUtil.scrypt(password, 16384, 8, 1);
        cachedPassword = password;
    }

    public boolean checkPassword(String password) {
        if (cachedPassword != null) {
            return cachedPassword.equals(password);
                
        }
        if (password == null) {
            return false;
        }
        if (SCryptUtil.check(password, cryptedPassword)) {
            cachedPassword = password;
            return true;
        }
        return false;
    }


    /** the unencrypted password; if this is non-null, it is checked
     * instead of cryptedPassword (which is costly) 
     */
    private String cachedPassword;

    public String email;

    /** The role is one of admin, calltaker, field trip scheduler, or teacher */
    public String role;

    public boolean isAdmin() {
        return role.equals("admin");
    }

}
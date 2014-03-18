package models.calltaker;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import models.TrinetUser;
import play.data.binding.As;
import play.db.jpa.Model;

/**
 *
 * @author demory
 */

@Entity
public class Call extends Model {
    
    @ManyToOne
    public TrinetUser user;
    
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date startTime;
    
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date endTime;

    public Call(TrinetUser user, Date startTime, Date endTime) {
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    @Override
    public String toString() {
        return String.format("call by user %s: %s to %s", user.username, startTime, endTime);
    }
}

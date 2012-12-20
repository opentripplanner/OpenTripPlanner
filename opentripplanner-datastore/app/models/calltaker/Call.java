package models.calltaker;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import play.data.binding.As;
import play.db.jpa.Model;

/**
 *
 * @author demory
 */

@Entity
public class Call extends Model {
    
    public String userName;
    
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date startTime;
    
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date endTime;

    public Call(String userName, Date startTime, Date endTime) {
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public String toString() {
        return "user "+userName+": "+startTime+" to "+endTime;                
    }
}

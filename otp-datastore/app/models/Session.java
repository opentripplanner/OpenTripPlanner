

package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import play.db.jpa.*;

/**
 *
 * @author demory
 */

@Entity
public class Session extends Model {
  
    public String sessionId;
    
    @ManyToOne
    public TrinetUser user;
    
    public Date timeStamp;
    
    public Session(String id, TrinetUser user) {
        
        this.sessionId = id;
        this.user = user;
        timeStamp = new Date();
    }

}

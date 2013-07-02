package models.fieldtrip;
 
import com.google.gson.annotations.Expose;
import javax.persistence.Entity;
import play.db.jpa.Model;
 
/**
A stop on a trip for a group
 */
@Entity
public class GroupTripStop extends Model {
    
    //@ManyToOne(optional=false)
    //public GroupTrip groupTrip;
    
    @Expose
    public int stopIndex;
    
    public GroupTripStop(int stopIndex) {
      this.stopIndex = stopIndex;
    }
}
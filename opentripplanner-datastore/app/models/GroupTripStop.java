package models;
 
import java.util.Date;
import javax.persistence.*;
 
import play.db.jpa.*;
 
/**
A stop on a trip for a group
 */
@Entity
public class GroupTripStop extends Model {
    @ManyToOne
    public GroupTrip groupTrip;
    public int stopIndex;
}
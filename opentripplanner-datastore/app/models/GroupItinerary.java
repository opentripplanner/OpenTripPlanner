package models;
 
import java.util.List;
import javax.persistence.*;
 
import play.db.jpa.*;
 
/**
   A list of trip segments for a particular field trip.
 */
@Entity
public class GroupItinerary extends Model {

    /** The trip that this belongs to */
    @ManyToOne(optional=false)
    public ScheduledFieldTrip fieldTrip;

    /** How many passengers are on this set of trips */
    @Column(nullable=false)
    public int passengers;

    @OneToMany(mappedBy="groupItinerary", cascade=CascadeType.ALL)
    public List<GroupTrip> trips;
}
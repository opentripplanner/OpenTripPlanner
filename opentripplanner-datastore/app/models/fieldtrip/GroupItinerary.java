package models.fieldtrip;
 
import java.util.List;
import javax.persistence.*;

import com.google.gson.annotations.Expose;
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
    @Expose
    public int passengers;

    @Column(length=10000, nullable=false)
    @Expose  
    public String itinData;
    
    @OneToMany(mappedBy="groupItinerary", cascade=CascadeType.ALL)
    //@OneToMany(cascade=CascadeType.ALL)
    @Expose
    public List<GTFSTrip> trips;
    
    public GroupItinerary(int passengers) {
        this.passengers = passengers;
    }
}
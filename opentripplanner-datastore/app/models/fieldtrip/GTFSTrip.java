package models.fieldtrip;
 
import com.google.gson.annotations.Expose;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import play.data.binding.As;
import play.db.jpa.Model;
 
/**
 *  A segment of a GTFS-defined trip which is part of a group itinerary
 */

@Entity
public class GTFSTrip extends Model {
 
    @ManyToOne(optional=false)
    public GroupItinerary groupItinerary;

    @Temporal(TemporalType.TIME)
    @Expose
    @As("HH:mm:ss")
    public Date depart;

    @Temporal(TemporalType.TIME)
    @Expose
    @As("HH:mm:ss")
    public Date arrive;
    
    @Expose
    public String agencyAndId;

    @Expose
    public String routeName;

    @Expose
    public Integer fromStopIndex;

    @Expose
    public Integer toStopIndex;

    @Expose
    public Integer blockId;

    @Expose
    public String fromStopName;
    
    @Expose
    public String toStopName;
    
    @Expose
    public String headsign;

    @Expose
    public Integer capacity;
        
    /*@ManyToOne
    @Expose
    private Trip trip;*/

    //@Column
    /** GTFS trips are stored in the system only once each */
    /*public void setTrip(Trip trip) {
        this.trip = trip.createInstance();
    }*/

    /** */
    //@OneToMany(mappedBy="groupTrip", cascade=CascadeType.ALL)
    /*@OneToMany(cascade=CascadeType.ALL)
    @Expose
    public List<GroupTripStop> stops;*/
    
    /*@Expose
    public List<Integer> stops;*/
}
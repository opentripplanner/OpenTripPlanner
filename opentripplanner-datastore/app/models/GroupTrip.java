package models;
 
import java.util.List;
import javax.persistence.*;
 
import play.db.jpa.*;
 
/**
A segment of a trip which is part of an itinerary
 */
@Entity
public class GroupTrip extends Model {
 
    @ManyToOne
    public GroupItinerary groupItinerary;

    @Temporal(TemporalType.TIME)
    public Date depart;

    @Temporal(TemporalType.TIME)
    public Date arrive;

    private Trip trip;

    @Column
    /** GTFS trips are stored in the system only once each */
    public void setTrip(Trip trip) {
        this.trip = trip.createInstance();
    }

    /** */
    @OneToMany(mappedBy="groupTrip", cascade=CascadeType.ALL)
    public List<GroupTripStop> stops;
}
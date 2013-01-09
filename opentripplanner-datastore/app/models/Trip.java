package models;

import com.google.gson.annotations.Expose;
import java.util.List;
import javax.persistence.*;
 
import play.db.jpa.*;
 
/**
A GTFS trip.  These should be lazily populated, since we don't want to
unnecessarily duplicate data from OTP.  Be sure to use createInstance.

*/
@Entity
@Table(
    uniqueConstraints=
        @UniqueConstraint(columnNames={"agencyId", "tripId"})
)
public class Trip extends Model {

    @Column(nullable=false)
    @Expose    
    public String agencyId;

    @Column(nullable=false)
    @Expose
    public String routeId;

    @Column(nullable=false)
    @Expose
    public String tripId;

    //@OneToMany(mappedBy="trip")
    //public List<GroupTrip> trips;

    public Trip createInstance() {
        try {
            save();
            return this;
        } catch (PersistenceException e) {
            JPA.em().clear();
            Trip trip = Trip.find("agencyId = ? and tripId = ?", agencyId, tripId).first();
            return trip;
        }
    }
}
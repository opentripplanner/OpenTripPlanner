package models;

import java.util.List;
import javax.persistence.*;
 
import play.db.jpa.*;
 
/**
A GTFS trip.  These are lazily populated, since we don't want to
unnecessarily duplicate data from OTP.  
*/
@Entity
public class Trip extends Model {

    public String routeId;

    public String tripId;

    @OneToMany(mappedBy="trip")
    public List<GroupTrip> trips;
}
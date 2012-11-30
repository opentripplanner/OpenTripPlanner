package models;
 
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.*;

import play.db.jpa.*;
 
@Entity
public class ScheduledFieldTrip extends Model {
 
    /** The username of the user who created this trip */
    @Column(nullable=false)
    public String createdBy;

    /** When the trip was created */
    @Column(nullable=false)
    public Date timeStamp;

    @PrePersist
    public void prePersist() {
        if (timeStamp == null) {
            timeStamp = new Date();
        }
    }

    /** When the trip is scheduled to depart, which is not 
        necessarily the time that any of the users actually depart.
        This is in local time.
    */
    @Temporal(TemporalType.TIME)
    @Column(nullable=false)
    public Date departure;

    /** Service day for the trip (service days are defined in local time, and it
        is assumed that all trips for a given field trip happen on the same service day) */
    @Temporal(TemporalType.DATE)
    @Column(nullable=false)
    public Date serviceDay;

    /**
       The origin/destination of the trip in OTP format (lat,lon::name)
     */
    public String origin, destination;

    /** A description of the trip ("Morx Elementary school 3rd grade trip to zoo") 
     */
    public String description;


    /** The name of the school.
     */
    public String school;

    /** The name of the teacher leading the trip.
     */
    public String teacher;

    @Column(nullable=false)
    public boolean mailed = false;

    /** The number of passengers on the trip.  A trip is not complete
        unless passengers == sum(passengers) over all group itineraries
     */
    public int passengers;

    /** The itineraries for this trip */
    @OneToMany(mappedBy="fieldTrip")
    public List<GroupItinerary> groupItineraries;
    

    public boolean isScheduled() {
        Query query = JPA.em().createQuery("select sum(passengers) from GroupItinerary where fieldTripId = ?");
        query.setParameter(0, this.id);
        return passengers == (int)query.getSingleResult();
    }


    public ScheduledFieldTrip(String createdBy, Date departure, String origin, String destination, 
                     String description, int passengers) {
        this.createdBy = createdBy;
        //fixme: not quite sure that this is safe
        this.departure = departure;
        this.serviceDay = departure;
        this.origin = origin;
        this.destination = destination;
        this.description = description;
        this.passengers = passengers;
    }

    public String toString() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(departure);
        String timeString = String.format("%d:%02d %s", calendar.get(Calendar.HOUR),
                                          calendar.get(Calendar.MINUTE),
                                          calendar.get(Calendar.AM_PM));
        String scheduled = isScheduled() ? "scheduled" : "unscheduled";
        return timeString + " " + description + " (" + passengers + " passengers) " + scheduled;
    }
}
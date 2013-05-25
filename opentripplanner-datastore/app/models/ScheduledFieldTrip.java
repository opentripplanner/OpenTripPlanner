package models;
 
import com.google.gson.annotations.Expose;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.*;
import play.data.binding.As;

import play.db.jpa.*;
 
@Entity
public class ScheduledFieldTrip extends GenericModel {
 
    //@Expose
    ////public long idCopy
            
    @Id
    @GeneratedValue
    @Expose
    public Long id;
    
    /** The username of the user who created this trip */
    @Column(nullable=false)
    @Expose
    public String createdBy;

    /** When the trip was created */
    @Column(nullable=false)
    @Expose
    public Date timeStamp;

    @Column(length=1000, nullable=false)
    @Expose
    public String queryParams;
    
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
    @As("yyyy-MM-dd'T'HH:mm:ss")
    @Expose
    public Date departure;

    /** Service day for the trip (service days are defined in local time, and it
        is assumed that all trips for a given field trip happen on the same service day) */
    @Temporal(TemporalType.DATE)
    @Column(nullable=false)
    @Expose
    public Date serviceDay;

    /**
       The origin/destination of the trip in OTP format (lat,lon::name)
     */
    @Expose
    public String origin, destination;

    /** A description of the trip ("Morx Elementary school 3rd grade trip to zoo") 
     */
    @Expose
    public String description;


    /** The name of the school.
     */
    public String school;

    /** The name of the teacher leading the trip.
     */
    public String teacher;

    @Column(nullable=false)
    @Expose
    public boolean mailed = false;

    /** The number of passengers on the trip.  A trip is not complete
        unless passengers == sum(passengers) over all group itineraries
     */
    @Expose
    public int passengers;

    /** The itineraries for this trip */
    @OneToMany(mappedBy="fieldTrip", cascade=CascadeType.ALL)
    @Expose
    public List<GroupItinerary> groupItineraries;
    

    public boolean isScheduled() {
        Query query = JPA.em().createQuery("select sum(passengers) from GroupItinerary where fieldTrip_Id = ?");
        query.setParameter(1, this.id);
        Object result = query.getSingleResult();
        if (result == null) {
            //for some idiotic reason, the sum of an empty list is null rather than zero.
            return false;
        }
        return passengers == (Integer) result;
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
                                          (calendar.get(Calendar.AM_PM) == 0 ? "AM" : "PM"));
        String scheduled = isScheduled() ? "scheduled" : "unscheduled";
        return timeString + " " + description + " (" + passengers + " passengers) " + scheduled;
    }
}
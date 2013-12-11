package models.fieldtrip;

import com.google.gson.annotations.Expose;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import play.data.binding.As;
import play.db.jpa.GenericModel;
import play.db.jpa.Model;
 

/**
 *
 * @author demory
 */


@Entity  
public class FieldTripRequest extends GenericModel  {

    @Id
    @GeneratedValue
    @Expose
    public Long id;
      
    @Expose
    public String teacherName;
    
    @Expose
    public String schoolName;

    @Expose
    public String grade;

    @Expose
    public String address;
    
    @Expose
    public String city;
    
    @Expose
    public String state;
    
    @Expose
    public String zip;

    @Expose
    public String phoneNumber;

    @Expose
    public String faxNumber;
        
    @Expose
    public String emailAddress;

    @Expose
    public String startLocation;

    @Expose
    public String endLocation;

    @Expose
    public String intermediateLocations;
    
    @Expose
    public Integer numStudents;

    @Expose
    public Integer numChaperones;

    @Expose
    public Integer minimumAge;

    @Expose
    public Integer maximumAge;

    @Expose
    @As("MM/dd/yyyy")
    public Date travelDate;

    @Expose
    @As("hh:mma")
    public Date arriveDestinationTime;
    
    @Expose
    @As("hh:mma")
    public Date leaveDestinationTime;

    @Expose
    @As("hh:mma")
    public Date arriveSchoolTime;

    @Expose
    public String paymentPreference;

    @Expose
    public Boolean requireInvoice;

    @Expose
    public String classpassId;

    /*@Expose
    @OneToOne(mappedBy="outboundRequest", cascade=CascadeType.ALL)
    public ScheduledFieldTrip outboundTrip;

    @Expose
    @OneToOne(mappedBy="inboundRequest", cascade=CascadeType.ALL)
    public ScheduledFieldTrip inboundTrip;*/
    
    @OneToMany(mappedBy="request", cascade=CascadeType.ALL)
    @Expose
    public List<ScheduledFieldTrip> trips;
    
    
    @OneToMany(mappedBy="request", cascade=CascadeType.ALL)
    @Expose
    public List<FieldTripFeedback> feedback;
        
    @OneToMany(mappedBy="request", cascade=CascadeType.ALL)
    @Expose
    public List<FieldTripNote> notes;

    @Expose
    @Lob
    public String submitterNotes;
    
    @Expose
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date timeStamp;
    
    @Expose
    public String status = "active";


  public FieldTripRequest() {
    this.timeStamp = new Date();
  }

    
}


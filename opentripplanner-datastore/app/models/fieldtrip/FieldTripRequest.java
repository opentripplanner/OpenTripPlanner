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
    public String emailAddress;

    @Expose
    public String startLocation;

    @Expose
    public String endLocation;

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
    public ScheduledFieldTrip outboundTrip;

    @Expose
    public ScheduledFieldTrip inboundTrip;
    
    @Expose
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date timeStamp;


  public FieldTripRequest() {
    this.timeStamp = new Date();
  }

    
}


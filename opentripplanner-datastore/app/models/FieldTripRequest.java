package models;

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
    public String startLocation;

    @Expose
    public String endLocation;

    @Expose
    public Integer groupSize;
    
    @Expose
    public Integer minAge;
    
    @Expose
    public Integer maxAge;

    @Expose
    public Date arriveTime;

    @Expose
    public Date tripDate;

    
}


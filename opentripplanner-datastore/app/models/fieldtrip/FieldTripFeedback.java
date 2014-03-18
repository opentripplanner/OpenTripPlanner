package models.fieldtrip;
 
import com.google.gson.annotations.Expose;
import java.util.Date;
import javax.persistence.*;
 
import play.db.jpa.*;
 
/**
   A comment on a field trip.
 */
@Entity
public class FieldTripFeedback extends Model {

    /** The trip that this belongs to */
    @ManyToOne
    public FieldTripRequest request;

    @Column
    @Expose
    public String userName;

    @Column(nullable=false)
    @Expose
    @Lob
    public String feedback;

    @Column(nullable=false)
    @Expose
    public Date timeStamp;

    @PrePersist
    public void prePersist() {
        if (timeStamp == null) {
            timeStamp = new Date();
        }
    }

}
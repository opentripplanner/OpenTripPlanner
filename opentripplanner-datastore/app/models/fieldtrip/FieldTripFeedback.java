package models.fieldtrip;
 
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
    public ScheduledFieldTrip fieldTrip;

    @Column(nullable=false)
    public String userName;

    @Column(nullable=false)
    @Lob
    public String feedback;

    @Column(nullable=false)
    public Date timestamp;
}
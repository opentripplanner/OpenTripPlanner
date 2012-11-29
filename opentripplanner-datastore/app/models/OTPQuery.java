package models;
 
import java.util.Date;
import javax.persistence.*;
 
import play.db.jpa.*;
 
@Entity
public class OTPQuery extends Model {
 
    public String userName, queryParams;
    public String fromPlace;
    public String toPlace;
    public Date timeStamp;
    
    public OTPQuery(String userName, String queryParams, String fromPlace, String toPlace) {
        this.userName = userName;
        this.queryParams = queryParams;
        this.fromPlace = fromPlace;
        this.toPlace = toPlace;
        this.timeStamp = new Date();
    }
    
    @Override
    public String toString() {
        return "["+id+"] query from "+fromPlace+" to "+toPlace+", submitted by "+userName+" at "+timeStamp+", json="+queryParams;
    }
 
}
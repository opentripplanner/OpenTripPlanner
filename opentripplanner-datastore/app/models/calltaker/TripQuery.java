/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package models.calltaker;

import java.util.Date;
import javax.persistence.*;
 
import play.data.binding.As;
import play.db.jpa.*;

/**
 *
 * @author demory
 */

 
@Entity
public class TripQuery extends Model {
 
    public String userName;
    
    @Column(length=1000)
    public String queryParams;
    
    public String fromPlace;
    public String toPlace;
    
    @As("yyyy-MM-dd'T'HH:mm:ss")
    public Date timeStamp;
    
    @ManyToOne
    public Call call;
    
    public TripQuery(String userName, String queryParams, String fromPlace, String toPlace) {
        this.userName = userName;
        this.queryParams = queryParams;
        this.fromPlace = fromPlace;
        this.toPlace = toPlace;
        this.timeStamp = new Date();
    }
    
    @Override
    public String toString() {
        return "["+id+"] query from "+fromPlace+" to "+toPlace+", submitted by "+userName+" at "+timeStamp+", call="+call;
    }
 
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package models.fieldtrip;

import com.google.gson.annotations.Expose;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import play.db.jpa.GenericModel;

/**
 *
 * @author demory
 */


@Entity
public class FieldTripNote extends GenericModel {

    @Id
    @GeneratedValue
    @Expose
    public Long id;
        
    /** The trip that this belongs to */
    @ManyToOne
    public FieldTripRequest request;

    @Column
    @Expose
    public String userName;

    @Column(nullable=false)
    @Expose
    @Lob
    public String note;

    @Column(nullable=false)
    @Expose
    public Date timeStamp;

    @Column
    @Expose
    public String type; // "internal" or "operational"
        
    @PrePersist
    public void prePersist() {
        if (timeStamp == null) {
            timeStamp = new Date();
        }
    }

}
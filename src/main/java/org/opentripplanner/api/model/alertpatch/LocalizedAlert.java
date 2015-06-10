package org.opentripplanner.api.model.alertpatch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.routing.alertpatch.Alert;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;
import java.util.Locale;

@XmlRootElement(name = "Alert")
public class LocalizedAlert {

    @XmlTransient
    @JsonIgnore
    public Alert alert;

    @XmlTransient
    @JsonIgnore
    private Locale locale;

    public LocalizedAlert(Alert alert, Locale locale) {
        this.alert = alert;
        this.locale = locale;
    }

    public LocalizedAlert(){
    }

    @XmlAttribute
    @JsonSerialize
    public String getAlertHeaderText() {
        if (alert.alertHeaderText == null) {
            return null;
        }
        return alert.alertHeaderText.toString(locale);
    }

    @XmlAttribute
    @JsonSerialize
    public String getAlertDescriptionText() {
        if (alert.alertDescriptionText == null) {
            return null;
        }
        return alert.alertDescriptionText.toString(locale);
    }

    @XmlAttribute
    @JsonSerialize
    public String getAlertUrl() {
        if (alert.alertUrl == null) {
            return null;
        }
        return alert.alertUrl.toString(locale);
    }

    //null means unknown
    @XmlElement
    @JsonSerialize
    public Date getEffectiveStartDate() {
        return alert.effectiveStartDate;
    }
}

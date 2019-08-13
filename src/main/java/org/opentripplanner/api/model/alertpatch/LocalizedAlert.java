package org.opentripplanner.api.model.alertpatch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.routing.alertpatch.Alert;

import java.util.Date;
import java.util.Locale;

public class LocalizedAlert {

    @JsonIgnore
    public Alert alert;

    @JsonIgnore
    private Locale locale;

    public LocalizedAlert(Alert alert, Locale locale) {
        this.alert = alert;
        this.locale = locale;
    }

    public LocalizedAlert(){
    }

    @JsonSerialize
    public String getAlertHeaderText() {
        if (alert.alertHeaderText == null) {
            return null;
        }
        return alert.alertHeaderText.toString(locale);
    }

    @JsonSerialize
    public String getAlertDescriptionText() {
        if (alert.alertDescriptionText == null) {
            return null;
        }
        return alert.alertDescriptionText.toString(locale);
    }

    @JsonSerialize
    public String getAlertUrl() {
        if (alert.alertUrl == null) {
            return null;
        }
        return alert.alertUrl.toString(locale);
    }

    //null means unknown
    @JsonSerialize
    public Date getEffectiveStartDate() {
        return alert.effectiveStartDate;
    }
}

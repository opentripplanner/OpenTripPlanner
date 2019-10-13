package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Alert implements Serializable {
    private static final long serialVersionUID = 8305126586053909836L;

    public I18NString alertHeaderText;
    public I18NString alertDescriptionText;
    public I18NString alertDetailText;
    public I18NString alertAdviceText;

    // TODO OTP2 we wanted to merge the GTFS single alertUrl and the SIRI multiple URLs.
    //      However, GTFS URLs are one-per-language in a single object, and SIRI URLs are N objects with no translation.
    public I18NString alertUrl;

    private List<AlertUrl> alertUrlList = new ArrayList<>();

    //null means unknown
    public Date effectiveStartDate;

    //null means unknown
    public Date effectiveEndDate;

    //null means unknown
    public String alertType;

    //null means unknown
    public String severity;

    public List<AlertUrl> getAlertUrlList() {
        return alertUrlList;
    }

    public void setAlertUrlList(List<AlertUrl> alertUrlList) {
        this.alertUrlList = alertUrlList;
    }

    public static Alert createSimpleAlerts(String text) {
        Alert note = new Alert();
        note.alertHeaderText = new NonLocalizedString(text);
        return note;
    }

    // TODO - Alerts should not be added to the internal model if they are the same; This check should be done
    //      - when importing the Alerts into the system. Then the Alert can use the System identity for
    //      - hachCode and equals.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alert alert = (Alert) o;
        return Objects.equals(alertHeaderText, alert.alertHeaderText) &&
                Objects.equals(alertDescriptionText, alert.alertDescriptionText) &&
                Objects.equals(alertDetailText, alert.alertDetailText) &&
                Objects.equals(alertAdviceText, alert.alertAdviceText) &&
                Objects.equals(alertUrl, alert.alertUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alertHeaderText, alertDescriptionText, alertDetailText, alertAdviceText, alertUrl);
    }

    @Override
    public String toString() {
        return "Alert('"
                + (alertHeaderText != null ? alertHeaderText.toString()
                        : alertDescriptionText != null ? alertDescriptionText.toString()
                        : alertDetailText != null ? alertDetailText.toString()
                        : alertAdviceText != null ? alertAdviceText.toString()
                                : "?") + "')";
    }
}

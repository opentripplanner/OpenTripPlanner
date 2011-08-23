package org.opentripplanner.routing.patch;

import java.io.Serializable;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Alert implements Serializable {
    private static final long serialVersionUID = 8305126586053909836L;

    @XmlElement
    public TranslatedString alertHeaderText;

    @XmlElement
    public TranslatedString alertDescriptionText;

    @XmlElement
    public TranslatedString alertUrl;

    public static HashSet<Alert> newSimpleAlertSet(String text) {
        Alert note = createSimpleAlerts(text);
        HashSet<Alert> notes = new HashSet<Alert>(1);
        notes.add(note);
        return notes;
    }

    public static Alert createSimpleAlerts(String text) {
        Alert note = new Alert();
        note.alertHeaderText = new TranslatedString();
        note.alertHeaderText.addTranslation("en", text);
        return note;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Alert)) {
            return false;
        }
        Alert ao = (Alert) o;
        if (alertDescriptionText == null) {
            if (ao.alertDescriptionText != null) {
                return false;
            }
        } else {
            if (!alertDescriptionText.equals(ao.alertDescriptionText)) {
                return false;
            }
        }
        if (alertHeaderText == null) {
            if (ao.alertHeaderText != null) {
                return false;
            }
        } else {
            if (!alertHeaderText.equals(ao.alertHeaderText)) {
                return false;
            }
        }
        if (alertUrl == null) {
            return ao.alertUrl == null;
        } else {
            return alertUrl.equals(ao.alertUrl);
        }
    }

    public int hashCode() {
        return (alertDescriptionText == null ? 0 : alertDescriptionText.hashCode())
                + (alertHeaderText == null ? 0 : alertHeaderText.hashCode())
                + (alertUrl == null ? 0 : alertUrl.hashCode());
    }
}

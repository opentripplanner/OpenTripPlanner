package org.opentripplanner.updater.street_notes;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Date;

/**
 * Example implementation of a WFS based street note updater, which can be used to retrieve roadworks and other
 * temporary obstacles from a WFS interface provided by the City of Helsinki's planning department.
 *
 * Usage example:
 *
 * <pre>
 *     winkki.type = winkki-polling-updater
 *     winkki.frequencySec = 21600
 *     winkki.url = http://geoserver.hel.fi/geoserver/hkr/ows?Service=wfs&Version=1.1.0&Request=GetCapabilities
 *     winkki.featureType = hkr:winkki_works
 * </pre>
 */


public class WinkkiPollingGraphUpdater extends WFSNotePollingGraphUpdater {
    protected Alert getNote(SimpleFeature feature) {
        Alert alert = Alert.createSimpleAlerts("winkki:" + feature.getAttribute("licence_type"));
        alert.alertDescriptionText = feature.getAttribute("event_description") == null ?
                new NonLocalizedString("") : new NonLocalizedString(feature.getAttribute("event_description").toString());
        alert.effectiveStartDate = feature.getAttribute("licence_startdate") == null ?
                (Date) feature.getAttribute("event_startdate") : (Date) feature.getAttribute("licence_startdate");
        return alert;
    }
}
package org.opentripplanner.updater.street_notes;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.util.NonLocalizedString;

import java.util.Date;

/**
 * Example implementation of a WFS based street note updater, which can be used to retrieve
 * roadworks and other temporary obstacles from a WFS interface provided by the City of Helsinki's
 * planning department.
 *
 * Usage example:
 *
 * <pre>
 *     winkki.type = winkki-polling-updater
 *     winkki.frequencySec = 21600
 *     winkki.url = http://geoserver.hel.fi/geoserver/hkr/ows?Service=wfs&amp;Version=1.1.0&amp;Request=GetCapabilities
 *     winkki.featureType = hkr:winkki_works
 * </pre>
 */


public class WinkkiPollingGraphUpdater extends WFSNotePollingGraphUpdater {
    public WinkkiPollingGraphUpdater(WFSNotePollingGraphUpdaterParameters config) {
        super(config);
    }

    protected StreetNote getNote(SimpleFeature feature) {
        StreetNote streetNote = new StreetNote(feature.getAttribute("licence_type").toString());
        streetNote.descriptionText = feature.getAttribute("event_description") == null ?
                new NonLocalizedString("") : new NonLocalizedString(feature.getAttribute("event_description").toString());
        streetNote.effectiveStartDate = feature.getAttribute("licence_startdate") == null ?
                (Date) feature.getAttribute("event_startdate") : (Date) feature.getAttribute("licence_startdate");
        streetNote.effectiveEndDate = feature.getAttribute("licence_enddate") == null ?
            (Date) feature.getAttribute("event_enddate") : (Date) feature.getAttribute("licence_enddate");
        return streetNote;
    }
}
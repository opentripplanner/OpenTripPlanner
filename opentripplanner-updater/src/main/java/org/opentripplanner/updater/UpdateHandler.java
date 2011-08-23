package org.opentripplanner.updater;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.AlertPatch;
import org.opentripplanner.routing.patch.TimePeriod;
import org.opentripplanner.routing.patch.TranslatedString;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;

public class UpdateHandler {
    private static final Logger log = LoggerFactory.getLogger(UpdateHandler.class);

    private FeedMessage message;

    private String defaultAgencyId;

    private Set<String> patchIds = new HashSet<String>();

    private Graph graph;

    private GraphService graphService;

    private PatchService patchService;
    
    public UpdateHandler(FeedMessage message) {
        this.message = message;
    }

    public void update() {
        graph = graphService.getGraph();
        for (FeedEntity entity : message.getEntityList()) {
            if (!entity.hasAlert()) {
                continue;
            }
            GtfsRealtime.Alert alert = entity.getAlert();
            String id = entity.getId();
            handleAlert(id, alert);
        }
        
        patchService.expireAllExcept(patchIds);
    }

    private void handleAlert(String id, GtfsRealtime.Alert alert) {
        Alert alertText = new Alert();
        alertText.alertDescriptionText = deBuffer(alert.getDescriptionText());
        alertText.alertHeaderText = deBuffer(alert.getHeaderText());
        alertText.alertUrl = deBuffer(alert.getUrl());
        ArrayList<TimePeriod> periods = new ArrayList<TimePeriod>();
        for (TimeRange activePeriod : alert.getActivePeriodList()) {
            final long start = activePeriod.hasStart() ? activePeriod.getStart() : 0;
            final long end = activePeriod.hasEnd() ? activePeriod.getEnd() : Long.MAX_VALUE;
            periods.add(new TimePeriod(start, end));
        }
        for (EntitySelector informed : alert.getInformedEntityList()) {
            String routeId = null;
            if (informed.hasRouteId()) {
                routeId = informed.getRouteId();
            }
            String stopId = null;
            if (informed.hasStopId()) {
                stopId = informed.getStopId();
            }

            String agencyId = informed.getAgencyId();
            if (informed.hasAgencyId()) {
                agencyId = informed.getAgencyId().intern();
            } else {
                agencyId = defaultAgencyId;
            }
            if (agencyId == null) {
                log.error("Empty agency id (and no default set) in feed; other ids are route "
                        + routeId + " and stop " + stopId);
                continue;
            }
            agencyId = agencyId.intern();

            AlertPatch patch = new AlertPatch();
            if (routeId != null) {
                patch.setRoute(new AgencyAndId(agencyId, routeId));
            }
            if (stopId != null) {
                patch.setStop(new AgencyAndId(agencyId, stopId));
            }
            patch.setTimePeriods(periods);
            patch.setId(id);
            patch.setAlert(alertText);
            patch.apply(graph);
            patchIds.add(id);
        }
    }

    /**
     * convert a protobuf TranslatedString to a OTP TranslatedString
     * 
     * @return
     */
    private TranslatedString deBuffer(GtfsRealtime.TranslatedString buffered) {
        TranslatedString result = new TranslatedString();
        for (Translation translation : buffered.getTranslationList()) {
            String language = translation.getLanguage();
            String string = translation.getText();
            result.addTranslation(language, string);
        }
        return result;
    }

    public void setDefaultAgencyId(String defaultAgencyId) {
        this.defaultAgencyId = defaultAgencyId.intern();
    }

    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    public void setPatchService(PatchService patchService) {
        this.patchService = patchService;
    }

}

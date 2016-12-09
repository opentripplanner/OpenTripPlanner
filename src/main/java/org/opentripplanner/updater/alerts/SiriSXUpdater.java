/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.updater.alerts;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.*;
import org.opentripplanner.util.HttpUtils;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.UUID;

public class SiriSXUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriSXUpdater.class);

    private GraphUpdaterManager updaterManager;

    private ZonedDateTime lastTimestamp = ZonedDateTime.now().minusWeeks(1);

    private String url;

    private String feedId;

    private SiriFuzzyTripMatcher fuzzyTripMatcher;

    private AlertPatchService alertPatchService;

    private long earlyStart;

    private AlertsUpdateHandler updateHandler = null;

    private String requestorRef;

    private int timeout;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        // TODO: add options to choose different patch services
        AlertPatchService alertPatchService = new AlertPatchServiceImpl(graph);
        this.alertPatchService = alertPatchService;
        String url = config.path("url").asText();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }

        this.requestorRef = config.path("requestorRef").asText();
        if (requestorRef == null || requestorRef.isEmpty()) {
            requestorRef = UUID.randomUUID().toString();
        }

        this.url = url;// + uniquenessParameter;
        this.earlyStart = config.path("earlyStartSec").asInt(0);
        this.feedId = config.path("feedId").asText();


        int timeoutSec = config.path("timeoutSec").asInt();
        if (timeoutSec > 0) {
            this.timeout = 1000*timeoutSec;
        }

        this.fuzzyTripMatcher = new SiriFuzzyTripMatcher(graph.index);

        LOG.info("Creating real-time alert updater (SIRI SX) running every {} seconds : {}", frequencySec, url);
    }

    @Override
    public void setup() {
        if (updateHandler == null) {
            updateHandler = new AlertsUpdateHandler();
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setFeedId(feedId);
        updateHandler.setAlertPatchService(alertPatchService);
        updateHandler.setSiriFuzzyTripMatcher(fuzzyTripMatcher);

        try {
            jaxbContext = JAXBContext.newInstance(Siri.class);
        } catch (JAXBException e) {

        }
    }

    JAXBContext jaxbContext;

    @Override
    protected void runPolling() {
        long t1 = System.currentTimeMillis();
        try {

            InputStream is = HttpUtils.postData(url, SiriXml.toXml(createSXServiceRequest(requestorRef)), timeout);

            Siri siri = (Siri)jaxbContext.createUnmarshaller().unmarshal(is);

            LOG.info("Fetching SX-data took {} ms", (System.currentTimeMillis()-t1));
            if (siri == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }
            ServiceDelivery serviceDelivery = siri.getServiceDelivery();
            if (serviceDelivery == null) {
                throw new RuntimeException("Failed to get serviceDelivery " + url);
            }

            ZonedDateTime responseTimestamp = serviceDelivery.getResponseTimestamp();
            if (responseTimestamp.isBefore(lastTimestamp)) {
                LOG.info("Ignoring feed with an old timestamp.");
                return;
            }

            // Handle update in graph writer runnable
            updaterManager.execute(new GraphWriterRunnable() {
                @Override
                public void run(Graph graph) {
                    updateHandler.update(serviceDelivery);
                }
            });

            lastTimestamp = responseTimestamp;
        } catch (Exception e) {
            LOG.info("Failed after {} ms", (System.currentTimeMillis()-t1));
            LOG.error("Error reading SIRI feed from " + url, e);
        }
    }


    private Siri createSXServiceRequest(String requestorRefValue) {
        Siri request = new Siri();
        request.setVersion("2.0");

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestTimestamp(ZonedDateTime.now());

        RequestorRef requestorRef = new RequestorRef();
        requestorRef.setValue(requestorRefValue);
        serviceRequest.setRequestorRef(requestorRef);

        SituationExchangeRequestStructure sxRequest = new SituationExchangeRequestStructure();
        sxRequest.setRequestTimestamp(ZonedDateTime.now());
        sxRequest.setVersion("2.0");

        MessageQualifierStructure messageIdentifier = new MessageQualifierStructure();
        messageIdentifier.setValue(UUID.randomUUID().toString());

        sxRequest.setMessageIdentifier(messageIdentifier);
        serviceRequest.getSituationExchangeRequests().add(sxRequest);

        request.setServiceRequest(serviceRequest);

        return request;
    }
    @Override
    public void teardown() {
    }

    public AlertPatchService getAlertPatchService() {
        return alertPatchService;
    }

    public String toString() {
        return "SiriSXUpdater (" + url + ")";
    }
}

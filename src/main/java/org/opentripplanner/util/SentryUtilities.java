package org.opentripplanner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.transit.realtime.GtfsRealtime;
import io.sentry.Sentry;
import io.sentry.context.Context;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.index.GraphQlPlanner;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SentryUtilities {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(GraphQlPlanner.class);

    static public void setupSentryTimetableSnapshot(final Graph graph, final boolean fullDataset, final String feedId, List<GtfsRealtime.TripUpdate> updates, boolean hasFuzzyMatcher) {
        Context sentryContext = Sentry.getContext();

        sentryContext.addExtra("graph buildTime", graph.buildTime);
        sentryContext.addExtra("feedId", feedId);
        sentryContext.addExtra("fullDataset", fullDataset);
        sentryContext.addExtra("hasFuzzyMatcher", hasFuzzyMatcher);


    }

    public static void setupSentryTripUpdate(GtfsRealtime.TripUpdate tripUpdate) {
        Context sentryContext = Sentry.getContext();
        sentryContext.addExtra("tripUpdate", tripUpdate.toString());
        sentryContext.addExtra("trip", tripUpdate.getTrip());
        sentryContext.addExtra("stopTimeUpdateCount", tripUpdate.getStopTimeUpdateCount());
        sentryContext.addExtra("timestamp", tripUpdate.getTimestamp());
    }

    static public void setupSentryFromMap(final Map<String, Object> extras) {
        Context sentryContext = Sentry.getContext();

        for (Map.Entry e : extras.entrySet()) {
            sentryContext.addExtra(e.getKey().toString(), e.getValue());
        }
        System.out.println(sentryContext.getExtra());
    }

    static public void setupSentryBeforePlan(final RoutingRequest request) {
        Context sentryContext = Sentry.getContext();
        sentryContext.addTag("router", request.routerId);
        sentryContext.addExtra("routerId", request.routerId);
        sentryContext.addExtra("numItineraries", request.numItineraries);
        sentryContext.addExtra("from", request.from.toDescriptiveString());
        sentryContext.addExtra("to", request.to.toDescriptiveString());

        if (request.intermediatePlaces != null) {
            sentryContext.addExtra("intermediatePlaces", request.intermediatePlaces.stream().map(intermediatePlace -> intermediatePlace.toDescriptiveString()).collect(Collectors.joining(", ")));
        }

        sentryContext.addExtra("time", request.getDateTime().toString());
        sentryContext.addExtra("unixTime", request.getDateTime().getTime());
        sentryContext.addExtra("maxWalkDistance", request.maxWalkDistance);
        sentryContext.addExtra("walkReluctance", request.walkReluctance);
        sentryContext.addExtra("walkSpeed", request.walkSpeed);
        sentryContext.addExtra("stairsReluctance", request.stairsReluctance);
        sentryContext.addExtra("walkOnStreetReluctance", request.walkOnStreetReluctance);
        sentryContext.addExtra("waitReluctance", request.waitReluctance);
        sentryContext.addExtra("transferPenalty", request.transferPenalty);
        sentryContext.addExtra("walkBoardCost", request.walkBoardCost);
        sentryContext.addExtra("minTransferTime", request.transferSlack);
        sentryContext.addExtra("maxTransfers", request.maxTransfers);
        sentryContext.addExtra("modes", request.modes.toString());
        sentryContext.addExtra("arriveBy", request.arriveBy);

        try {
            sentryContext.addExtra("parameters", mapper.writeValueAsString(request.parameters));
        } catch (JsonProcessingException e) {
            LOG.warn("Unable to serialize request parameters");
        }
    }

    static public void setupSentryAfterPlan(final RoutingRequest request, final TripPlan plan, final List<Message> messages) {
        Context sentryContext = Sentry.getContext();
        sentryContext.addExtra("resultNumItineraries", plan.itinerary.size());


        if (request.rctx != null) {
            sentryContext.addExtra("aborted", request.rctx.aborted);
            sentryContext.addExtra("timedOut", request.rctx.debugOutput.timedOut);


            try {
                sentryContext.addExtra("debugOutput", mapper.writeValueAsString(request.rctx.debugOutput));
            } catch (JsonProcessingException e) {
                LOG.warn("Unable to serialize debugOutput");
            }
        }
        sentryContext.addExtra("messages", Arrays.toString(messages.toArray()));

        if (plan.itinerary.isEmpty()) {
            EventBuilder eventBuilder = new EventBuilder()
                    .withMessage("Zero routes found")
                    .withLevel(Event.Level.ERROR)
                    .withLogger(LOG.getName());
            Sentry.capture(eventBuilder);
        }
    }


}

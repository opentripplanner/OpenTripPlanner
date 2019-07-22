package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.PatternDepartVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * This class calculates fares for the Southeast Queensland region, centered on Brisbane.
 * It uses the stop zone codes included in the GTFS feeds, but the fare data is hard-coded into this class based on
 * https://translink.com.au/tickets-and-fares/fares-and-zones/current-fares
 * https://translink.com.au/sites/default/files/assets/resources/plan-your-journey/maps/190107-seq-fare-zone.pdf
 */
public class BrisbaneFareService implements FareService {

    private static final Logger LOG = LoggerFactory.getLogger(BrisbaneFareService.class);

    // These are multiplicative factors, i.e. 0.8 means 80% of full fare. Concession and off-peak can be combined.
    private static final double GO_CARD_OFF_PEAK_PRICE_FACTOR = 0.8;
    private static final double CONCESSION_PRICE_FACTOR = 0.5;

    // Maps from number of zones to standard adult fare in AUD cents.
    TIntIntMap cardFares = new TIntIntHashMap();
    TIntIntMap paperFares = new TIntIntHashMap();

    /**
     * Constructor
     */
    public BrisbaneFareService() {
        // Initialize fare table with data for South East Queensland, dated 7 January 2019.
        cardFares.put(1,  331);
        cardFares.put(2,  403);
        cardFares.put(3,  616);
        cardFares.put(4,  811);
        cardFares.put(5, 1066);
        cardFares.put(6, 1353);
        cardFares.put(7, 1682);
        cardFares.put(8, 1996);
        paperFares.put(1,  480);
        paperFares.put(2,  580);
        paperFares.put(3,  890);
        paperFares.put(4, 1180);
        paperFares.put(5, 1550);
        paperFares.put(6, 1960);
        paperFares.put(7, 2440);
        paperFares.put(8, 2890);
    }

    @Override
    public Fare getCost(GraphPath path) {

        // Switch to `new TIntArrayList();` to log a full list of zones.
        TIntCollection zonesTraveled = new TIntHashSet();

        // Get the zone number for every state that is on board transit. Fares are based on all zones you pass through.
        for (State state : path.states) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof PatternDepartVertex) {
                PatternDepartVertex departVertex = ((PatternDepartVertex) vertex);
                Stop stop = departVertex.getStop();
                String zoneString = stop.getZoneId();
                // Check for stops on a zone border, which have zone codes formatted like "1/2" or "2/3".
                // We should obtain the correct results by disregarding such stops entirely, since travel from such
                // stops will always proceed through other single-zone stops in one of the two adjacent zones.
                if (zoneString.length() == 3 && zoneString.contains("/")) {
                    continue;
                }
                try {
                    int zoneInt = Integer.parseInt(zoneString);
                    zonesTraveled.add(zoneInt);
                } catch (NumberFormatException ex) {
                    LOG.warn("Could not parse integer zone ID: {}", zoneString);
                }
            }
        }

        Fare fare = new Fare();
        if (zonesTraveled.isEmpty()) {
            LOG.info("Path did not pass through any zones. The fare is undefined.");
            return fare;
        }
        int minZone = Integer.MAX_VALUE;
        int maxZone = Integer.MIN_VALUE;
        for (int zone : zonesTraveled.toArray()) {
            maxZone = Math.max(maxZone, zone);
            minZone = Math.min(minZone, zone);
        }

        int nZones = maxZone - minZone + 1;
        LOG.info("Max zone {}, min zone {}, crossed {} zones.", maxZone, minZone, nZones);
        // OTP has no multi-dimensional fare types. For now just return card fare (no paper fare)
        double cardFare = cardFares.get(nZones);
        if (!isPeak(path)) {
            cardFare = GO_CARD_OFF_PEAK_PRICE_FACTOR * cardFare;
        }
        double concessionCardFare = cardFare * CONCESSION_PRICE_FACTOR;
        fare.addFare(Fare.FareType.regular, new WrappedCurrency("AUD"), (int) Math.round(cardFare));
        fare.addFare(Fare.FareType.special, new WrappedCurrency("AUD"), (int) Math.round(concessionCardFare));
        return fare;
    }

    private static final ZoneId BRISBANE_TIME_ZONE = ZoneId.of("Australia/Brisbane");
    private final static LocalTime MORNING_PEAK_STARTS = LocalTime.of(6, 00);
    private final static LocalTime MORNING_PEAK_ENDS = LocalTime.of(8, 30);
    private final static LocalTime EVENING_PEAK_STARTS = LocalTime.of(15, 30);
    private final static LocalTime EVENING_PEAK_ENDS = LocalTime.of(19, 00);

    /**
     * Determine whether the given GraphPath should be assigned a peak or off-peak price.
     * https://translink.com.au/tickets-and-fares/fares-and-zones/off-peak-times
     *
     * "Off-peak fares are based on when you touch on." That's simple enough.
     * "If you transfer between services on a multi-trip journey across peak and off-peak times, your go card will
     * automatically calculate the fare as a combination of peak and off-peak." Yes, but how exactly? For now we will
     * just give a peak price if any boarding happens during peak period.
     */
    private boolean isPeak (GraphPath path) {
        boolean hasAnyPeakBoarding = false;
        for (State state : path.states) {
            if (state.getVertex() instanceof TransitStopDepart) {
                long secondsSinceEpoch = state.getTimeSeconds();
                hasAnyPeakBoarding |= isPeak(secondsSinceEpoch);
            }
        }
        return hasAnyPeakBoarding;
    }

    /**
     * Determine whether the given absolute time is within peak period or not.
     */
    private boolean isPeak (long secondsSinceEpoch) {
        Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
        ZonedDateTime zonedDateTime = instant.atZone(BRISBANE_TIME_ZONE);
        // First check for weekends, which are always off peak.
        DayOfWeek dayOfWeek = zonedDateTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        // This is a weekday. Check whether the time falls in the morning or evening peak windows.
        // We perform half-open interval checking with an "is-not-before" predicate.
        LocalTime localTime = zonedDateTime.toLocalTime();
        if (!localTime.isBefore(MORNING_PEAK_STARTS) && localTime.isBefore(MORNING_PEAK_ENDS)) {
            return true;
        }
        if (!localTime.isBefore(EVENING_PEAK_STARTS) && localTime.isBefore(EVENING_PEAK_ENDS)) {
            return true;
        }
        return false;
    }

    public static class Factory implements FareServiceFactory {
        @Override
        public FareService makeFareService() {
            return new BrisbaneFareService();
        }

        @Override
        public void processGtfs(OtpTransitService transitService) {

        }

        @Override
        public void configure(JsonNode config) {

        }
    }


}

package org.opentripplanner.updater.stoptime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.CTX;
import org.opentripplanner.routing.trippattern.Update;

public class KV8Update extends Update {

    public KV8Update(AgencyAndId tripId, String stopId, int stopSeq, int arrive, int depart,
            Status status, long timestamp) {
        super(tripId, stopId, stopSeq, arrive, depart, status, timestamp);
    }

    public static List<Update> fromCTX(String ctxString) {
        CTX ctx = new CTX(ctxString);
        return fromCTX(ctx);
    }
 
    public static List<Update> fromCTX(CTX ctx) {
        //LOG.trace(ctxString);
        // at this point, updates may have mixed trip IDs, dates, etc.
        List<Update> ret = new ArrayList<Update>(); 
        for (int i = 0; i < ctx.rows.size(); i++) {
            HashMap<String, String> row = ctx.rows.get(i);
            // there was a field in the CTX all along that indicated the extra non-passenger stops...
            if (row.get("JourneyStopType").equals("INFOPOINT"))
                continue;
            int arrival = secondsSinceMidnight(row.get("ExpectedArrivalTime"));
            int departure = secondsSinceMidnight(row.get("ExpectedDepartureTime"));
            KV8Update u = new KV8Update(
                    kv8TripId(row),   
                    kv8StopId(row), 
                    Integer.parseInt(row.get("UserStopOrderNumber")), 
                    arrival, departure,
                    kv8Status(row),
                    kv8Timestamp(row)) ;
            ret.add(u);
        }
        return ret;
    }

    /** 
     * no good for DST 
     */
    private static int secondsSinceMidnight(String hhmmss) {
        String[] time = hhmmss.split(":");
        int hours = Integer.parseInt(time[0]);
        int minutes = Integer.parseInt(time[1]);
        int seconds = Integer.parseInt(time[2]);
        return (hours * 60 + minutes) * 60 + seconds;
    }
    
    /** 
     * convert KV7 fields into a GTFS trip_id
     * trip_ids must be data set unique in GTFS, which is why we use the DataOwnerCode (~=agency_id) 
     * twice, in the trip_id itself and the enclosing AgencyAndId.
     * https://github.com/skywave/kv7tools/blob/master/kv7_gtfs.sql#L42
     */
    private static AgencyAndId kv8TripId (HashMap<String, String> row) {
        String tripId = String.format("%s_%s_%s_%s_%s",
                row.get("DataOwnerCode"),
                row.get("LinePlanningNumber"),
                row.get("LocalServiceLevelCode"),
                row.get("JourneyNumber"),
                row.get("FortifyOrderNumber"));
        return new AgencyAndId(row.get("DataOwnerCode"), tripId);
    }
    
    /** 
     * Convert KV7 fields into a GTFS stop_id. DataOwnerCode and UserStopCode are the agency's 
     * internal identifiers for a stop, so should not be used. TimingPointCode is a unique 
     * nationwide (feed-wide) identifier which includes those UserStopCodes.
     */
    private static String kv8StopId (HashMap<String, String> row) {
        return row.get("TimingPointCode");
    }
    
    private static long kv8Timestamp (HashMap<String, String> row) {
        String timestamp = row.get("LastUpdateTimeStamp");
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis();
        DateTime dt = parser.parseDateTime(timestamp);
        return dt.getMillis();
    }

    private static Update.Status kv8Status(HashMap<String, String> row) {
        String s = row.get("TripStopStatus");
        if (s.equals("DRIVING"))
            return Update.Status.PREDICTION;
        else
            return Update.Status.valueOf(s);
    }

}

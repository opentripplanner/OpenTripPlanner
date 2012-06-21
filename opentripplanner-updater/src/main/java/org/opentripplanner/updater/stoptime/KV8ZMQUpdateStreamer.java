package org.opentripplanner.updater.stoptime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.CTX;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.UpdateList;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/** StoptimeUpdateStreamer for CTX-encoded Dutch KV8 realtime updates over ZeroMQ */
public class KV8ZMQUpdateStreamer implements UpdateStreamer {

    private ZMQ.Context context;
    private ZMQ.Socket subscriber;
    private int count = 0;
    private String defaultAgencyId = "";
    private String address = "tcp://node01.post.openov.nl:7817";
    private static String feed = "/GOVI/KV8"; 
    
    public KV8ZMQUpdateStreamer() {
        context = ZMQ.context(1);
        subscriber = context.socket(ZMQ.SUB);
        subscriber.connect(address);
        subscriber.subscribe(feed.getBytes());
    }
    
    public UpdateList getUpdates() {
        ZMsg msg = ZMsg.recvMsg(subscriber);
        try {
            Iterator<ZFrame> msgs = msg.iterator();
            msgs.next();
            ArrayList<Byte> receivedMsgs = new ArrayList<Byte>();
            while (msgs.hasNext()) {
                for (byte b : msgs.next().getData()) {
                    receivedMsgs.add(b);
                }
            }
            byte[] fullMsg = new byte[receivedMsgs.size()];
            for (int i = 0; i < fullMsg.length; i++) {
                fullMsg[i] = receivedMsgs.get(i);
            }
            InputStream gzipped = new ByteArrayInputStream(fullMsg);
            InputStream in = new GZIPInputStream(gzipped);
            StringBuffer out = new StringBuffer();
            byte[] b = new byte[4096];
            for (int n; (n = in.read(b)) != -1;) {
                out.append(new String(b, 0, n));
            }
            return parseCTX(out.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public UpdateList parseCTX(String ctxString) {
        System.out.println("CTX MSG " + count++);
        CTX ctx = new CTX(ctxString);
        UpdateList ret = new UpdateList(null); // indicate that updates may have mixed trip IDs
        for (int i = 0; i < ctx.rows.size(); i++) {
            HashMap<String, String> row = ctx.rows.get(i);
            int arrival = secondsSinceMidnight(row.get("ExpectedArrivalTime"));
            int departure = secondsSinceMidnight(row.get("ExpectedDepartureTime"));
            Update u = new Update(
                    kv7TripId(row),   
                    row.get("UserStopCode"), 
                    Integer.parseInt(row.get("UserStopOrderNumber")), 
                    arrival, departure);
            ret.addUpdate(u);
        }
        return ret;
    }

    /** no good for DST */
    private int secondsSinceMidnight(String hhmmss) {
        String[] time = hhmmss.split(":");
        int hours = Integer.parseInt(time[0]);
        int minutes = Integer.parseInt(time[1]);
        int seconds = Integer.parseInt(time[2]);
        return (hours * 60 + minutes) * 60 + seconds;
    }
    
    /** 
     * convert KV7 fields into a GTFS trip_id
     * trip_ids must be data set unique in GTFS, which is why we use the DataOwnerCode (~=agency_id) 
     * twice, in the trip_id itself and the enclosing AgencyAndId
     */
    public AgencyAndId kv7TripId (HashMap<String, String> row) {
        String tripId = String.format("%s_%s_%s_%s_%s",
                row.get("DataOwnerCode"),
                row.get("LinePlanningNumber"),
                row.get("LocalServiceLevelCode"),
                row.get("JourneyNumber"),
                row.get("FortifyOrderNumber"));
        return new AgencyAndId(row.get("DataOwnerCode"), tripId);
    }
    
    public void setAddress(String address) {
        this.address = address;
    }

}

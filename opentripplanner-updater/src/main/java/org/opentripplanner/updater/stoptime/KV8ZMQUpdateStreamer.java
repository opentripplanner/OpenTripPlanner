package org.opentripplanner.updater.stoptime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.CTX;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.UpdateList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/** StoptimeUpdateStreamer for CTX-encoded Dutch KV8 realtime updates over ZeroMQ */
public class KV8ZMQUpdateStreamer implements UpdateStreamer {

    private static Logger LOG = LoggerFactory.getLogger(KV8ZMQUpdateStreamer.class); 
    
    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
    private long count = 0;
    @Setter private String defaultAgencyId = "";
    @Setter private String address = "tcp://node01.post.openov.nl:7817";
    @Setter private static String feed = "/GOVI/KV8"; 
    @Setter private static String messageLogFile;
    
    Writer logWriter;
    
    @PostConstruct
    public void connectToFeed() {
        subscriber.connect(address);
        subscriber.subscribe(feed.getBytes());
        if (messageLogFile != null) {
            try {
                logWriter = new FileWriter(messageLogFile);
            } catch (IOException e) {
                LOG.warn("problem opening message log file: {}", e);
                logWriter = null;
            }
        }
    }
    
    public UpdateList getUpdates() {
        /* recvMsg blocks -- unless you call Socket.setReceiveTimeout() */
        // so when timeout occurs, it does not return null, but a reference to some
        // static ZMsg object?
        ZMsg msg = ZMsg.recvMsg(subscriber);
        if (msg == null) {
            /* According to docs, null indicates that receive operation was "interrupted". */
            LOG.warn("ZMQ received null message.");
            return null;
        }
        /* 
         * on subscription failure, message will not be null or empty, but its content length 
         * will be 0 and bomb the gunzip below (or does it block forever?)
         */        
        UpdateList ret = null;
        try {
            Iterator<ZFrame> frames = msg.iterator();
            // pop off first frame, which contains "/GOVI/KV8" (the feed name) (isn't there a method for this?)
            frames.next();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(); 
            while (frames.hasNext()) {
                ZFrame frame = frames.next();
                byte[] frameData = frame.getData();
                buffer.write(frameData);
            }
            if (buffer.size() == 0) {
                LOG.debug("received 0-length CTX message {}", msg);
                return null;
            }
            // chain input streams to gunzip contents of byte buffer
            InputStream gzippedMessageStream = new ByteArrayInputStream(buffer.toByteArray());
            InputStream messageStream = new GZIPInputStream(gzippedMessageStream);
            // copy input stream back to output stream
            buffer.reset();
            byte[] b = new byte[4096];
            for (int n; (n = messageStream.read(b)) != -1;) {
                buffer.write(b, 0, n);
            }   
            if (logWriter != null) {
                logWriter.write(buffer.toString());
                logWriter.append('\n');
            }
            ret = parseCTX(buffer.toString());
            if (++count % 1 == 0) {
                LOG.debug("decoded gzipped CTX message #{}: {}", count, msg);
            }
        } catch (Exception e) {
            LOG.error("exception while decoding (unzipping) incoming CTX message: {}", e.getMessage()); 
        } finally {
            msg.destroy(); // is this necessary?
        }
        return ret;
    }
    
    public UpdateList parseCTX(String ctxString) {
        //LOG.debug(ctxString);
        CTX ctx = new CTX(ctxString);
        UpdateList ret = new UpdateList(null); // indicate that updates may have mixed trip IDs
        for (int i = 0; i < ctx.rows.size(); i++) {
            HashMap<String, String> row = ctx.rows.get(i);
            int arrival = secondsSinceMidnight(row.get("ExpectedArrivalTime"));
            int departure = secondsSinceMidnight(row.get("ExpectedDepartureTime"));
            Update u = new Update(
                    kv7TripId(row),   
                    kv7StopId(row), 
                    Integer.parseInt(row.get("UserStopOrderNumber")), 
                    arrival, departure,
                    kv8Status(row));
            ret.addUpdate(u);
        }
        return ret;
    }

    /** 
     * no good for DST 
     */
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
    
    /** 
     * Convert KV7 fields into a GTFS stop_id. DataOwnerCode and UserStopCode are the agency's 
     * internal identifiers for a stop, so should not be used. TimingPointCode is a unique 
     * nationwide (feed-wide) identifier which includes those UserStopCodes.
     */
    public String kv7StopId (HashMap<String, String> row) {
        return row.get("TimingPointCode");
    }
    
    public Update.Status kv8Status(HashMap<String, String> row) {
        String s = row.get("TripStopStatus");
        if (s.equals("DRIVING"))
            return Update.Status.PREDICTION;
        else
            return Update.Status.valueOf(s);
    }

}

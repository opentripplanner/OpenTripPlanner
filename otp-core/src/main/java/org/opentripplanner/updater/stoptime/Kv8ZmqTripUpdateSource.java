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

package org.opentripplanner.updater.stoptime;

import lombok.Setter;
import org.jeromq.ZFrame;
import org.jeromq.ZMQ;
import org.jeromq.ZMsg;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.updater.PreferencesConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;

/**
 * TripUpdateSource for CTX-encoded Dutch KV8 realtime updates over ZeroMQ
 */
public class Kv8ZmqTripUpdateSource implements TripUpdateSource, PreferencesConfigurable {

    private static Logger LOG = LoggerFactory.getLogger(Kv8ZmqTripUpdateSource.class); 
    
    private ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
    private long count = 0;
    @Setter private String address = "tcp://node01.post.openov.nl:7817";
    @Setter private static String feed = "/GOVI/KV8"; 
    @Setter private static String messageLogFile;
    @Setter private static int logFrequency = 2000;

    @Setter private static String fakeInput = null; //"/home/abyrd/nl.ctx";
    Writer logWriter;
    Reader fakeInputReader;
    
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
        if (fakeInput != null) {
            try {
                fakeInputReader = new FileReader(fakeInput);
            } catch (IOException e) {
                LOG.warn("problem opening fake input file: {}", e);
                fakeInputReader = null;
            }
        }
    }

    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        String address = preferences.get("address", null);
        if (address == null)
            throw new IllegalArgumentException("Missing mandatory 'address' parameter");
        setAddress(address);
        String feed = preferences.get("feed", null);
        if (feed == null)
            throw new IllegalArgumentException("Missing mandatory 'feed' parameter");
        setFeed(feed);
        setLogFrequency(preferences.getInt("logFrequency", 2000));
        setFakeInput(preferences.get("fakeInput", null));
        // Connect to feed
        connectToFeed();
    }
    
    public List<TripUpdateList> getUpdates() {
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
        List<Update> ret = null;
        try {
            String kv8ctx = gunzipMultifameZMsg(msg);
            if (logWriter != null) {
                logWriter.write(kv8ctx);
            }
            ret = Kv8Update.fromCTX(kv8ctx);
            count += 1; // if we got here there must not have been an exception
            LOG.debug("decoded gzipped CTX message #{}: {}", count, msg);
            if (count % logFrequency == 0) {
                LOG.info("received {} KV8 messages.", count);
            }
        } catch (Exception e) {
            LOG.error("exception while decoding (unzipping) incoming CTX message: {}", e.getMessage()); 
            e.printStackTrace();
        } finally {
            msg.destroy(); // is this necessary? does ZMQ lib automatically free mem?
        }
        return TripUpdateList.splitByTrip(ret);
    }
    
    private static String gunzipMultifameZMsg(ZMsg msg) throws IOException {
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
        return buffer.toString();
    }
}

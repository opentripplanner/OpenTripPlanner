package org.opentripplanner.updater.stoptime;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

import lombok.AllArgsConstructor;
import lombok.Setter;

import org.opentripplanner.updater.PreferencesConfigurable;
import org.opentripplanner.routing.graph.Graph;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.DefaultWebSocketListener;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

/**
 * This class starts an HTTP client which opens a websocket connection to a GTFS-RT data source.
 * A callback is registered which handles incoming GTFS-RT messages as they stream in by placing
 * a GTFS-RT decoder Runnable task in the single-threaded executor for handling. 
 * 
 * This should eventually be modified to use the same executor that is used for periodic graph
 * updater tasks, such that all writes to the graph are performed by a single thread.
 * 
 * @author abyrd
 */
public class WebsocketGTFSRealtimeUpdater implements PreferencesConfigurable {
    
    /** The URL at which the websocket stream of GTFS-RT messages will be found. */
    @Setter private String url;

    /** The executor that will schedule the task of decoding incoming messages. */
    @Setter private Executor executor; 
    
    public void start () {
        // The AsyncHttpClient library uses Netty by default (it has a dependency on Netty).
        // It can also make use of Grizzly for the HTTP layer, but the Jersey-Grizzly integration
        // forces us to use a version of Grizzly that is too old to be compatible with the current 
        // AsyncHttpClient. This would be done as follows:
        // AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().build();
        // AsyncHttpClient client = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(config), config);        
        // Using Netty by default:
        AsyncHttpClient client = new AsyncHttpClient(); 
        WebSocketListener listener = new Listener();
        WebSocketUpgradeHandler handler = 
                new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build();
        WebSocket socket;
        try {
            socket = client.prepareGet(url).execute(handler).get();
            //socket.sendMessage("I CAN HAZ GTFS-RT?".getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // client.closeAsynchronously();
    }
    
    public class Listener extends DefaultWebSocketListener {
        private int n_received = 0;
        private long last_log_time = System.currentTimeMillis();
        @Override
        public void onMessage(byte[] message) {
            n_received += 1;
            // System.out.println("Received message number " + n_received);
            if (n_received % 100 == 0) {
                long this_log_time = System.currentTimeMillis();
                long elapsed_time = this_log_time - last_log_time;
                System.out.printf("Received %d messages in %d msec, %f msgs/sec \n",
                        n_received, elapsed_time, n_received / (elapsed_time / 1000d));
                n_received = 0;
                last_log_time = System.currentTimeMillis();
            }
                
            Runnable runnable = new DecoderTask(message);
            executor.execute(runnable);
        }
    }
    
    @AllArgsConstructor
    public static class DecoderTask implements Runnable { //, GraphUpdaterRunnable 
        private byte[] bytes;
        @Override public void run() {
            System.out.println("Decoding message: " + bytes);
            UpdateStreamer us = new UpdateStreamer(bytes);
            System.out.println("Updates: " + us.getUpdates());
        }
    }

    @AllArgsConstructor
    public static class UpdateStreamer extends GtfsRealtimeAbstractUpdateStreamer {
        private byte[] bytes;
        @Override
        protected FeedMessage getFeedMessage() {
            FeedMessage feed = null;
            try {
                System.out.println("Parsing protocol buffers from message: " + bytes);
                feed = GtfsRealtime.FeedMessage.PARSER.parseFrom(bytes);
                // System.out.println("Feed: " + feed);
                return feed;
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            return null;
        }
        
    }
    
    public static void main (String[] argv) {
        WebsocketGTFSRealtimeUpdater wu = new WebsocketGTFSRealtimeUpdater();
        wu.setUrl("ws://localhost:8088/tripUpdates");
        wu.setExecutor(Executors.newSingleThreadExecutor());
        wu.start();
    }

    /** This method configures the WebsocketGTFSRealtimeUpdater based on Java Preferences */
    @Override
    public void configure(Graph graph, Preferences preferences) throws Exception {
        // TODO Auto-generated method stub        
    }
    
}

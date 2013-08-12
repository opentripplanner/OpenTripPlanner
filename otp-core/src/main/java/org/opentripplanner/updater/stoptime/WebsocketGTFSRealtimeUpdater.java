package org.opentripplanner.updater.stoptime;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import lombok.AllArgsConstructor;
import lombok.Setter;

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
public class WebsocketGTFSRealtimeUpdater {
    
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
        // Using Netty by default instead:
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
        @Override
        public void onMessage(byte[] message) {
            System.out.println("Received message: " + message);
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
                feed = GtfsRealtime.FeedMessage.parseFrom(bytes);
                System.out.println("Feed: " + feed);
                return feed;
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
            return null;
        }
        
    }
    
    public static void main (String[] argv) {
        WebsocketGTFSRealtimeUpdater wu = new WebsocketGTFSRealtimeUpdater();
//        wu.setUrl("ws://echo.websocket.org");
        wu.setUrl("ws://ovapi.nl:8088/tripUpdates");
        wu.setExecutor(Executors.newSingleThreadExecutor());
        wu.start();
    }
    
}

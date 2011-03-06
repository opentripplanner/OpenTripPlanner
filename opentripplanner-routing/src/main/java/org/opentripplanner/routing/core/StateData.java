package org.opentripplanner.routing.core;

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;

public class StateData {

    private final int trip;

    private final AgencyAndId tripId;

    private final double walkDistance;

    private final String zone;

    private final AgencyAndId route;

    private final FareContext fareContext;

    private final int numBoardings;

    private final boolean alightedLocal;

    private final boolean everBoarded;

    private final Vertex previousStop;

    private final long lastAlightedTime;

    private final Map<Object, Object> extensions;

    private StateData(Editor editor) {
        this.trip = editor.trip;
        this.tripId = editor.tripId;
        this.walkDistance = editor.walkDistance;
        this.zone = editor.zone;
        this.route = editor.route;
        this.fareContext = editor.fareContext;
        this.numBoardings = editor.numBoardings;
        this.alightedLocal = editor.alightedLocal;
        this.everBoarded = editor.everBoarded;
        this.previousStop = editor.previousStop;
        this.lastAlightedTime = editor.lastAlightedTime;
        this.extensions = editor.extensions;
    }

    public static Editor editor() {
        return new Editor();
    }

    public static StateData createDefault() {
        return editor().createData();
    }

    public Editor edit() {
        return new Editor(this);
    }

    public int getTrip() {
        return trip;
    }

    public AgencyAndId getTripId() {
        return tripId;
    }

    public double getWalkDistance() {
        return walkDistance;
    }

    public String getZone() {
        return zone;
    }

    public AgencyAndId getRoute() {
        return route;
    }

    public FareContext getFareContext() {
        return fareContext;
    }

    public int getNumBoardings() {
        return numBoardings;
    }

    public boolean isAlightedLocal() {
        return alightedLocal;
    }

    public boolean isEverBoarded() {
        return everBoarded;
    }

    public Vertex getPreviousStop() {
        return previousStop;
    }

    public long getLastAlightedTime() {
        return lastAlightedTime;
    }

    /**
     * Determine if a particular extension parameter is present for the specified key.
     * 
     * @param key
     * @return
     */
    public boolean containsExtension(Object key) {
        return extensions.containsKey(key);
    }

    /**
     * Get the extension parameter with the specified key.
     * 
     * @param <T>
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Object key) {
        return (T) extensions.get(key);
    }

    public static class Editor {

        /**
         * Time is not technically a member of StateData, but we add it as a convenience here for
         * easier state creation
         */
        private long time;

        private int trip = -1;

        private AgencyAndId tripId = null;

        private double walkDistance = 0;

        private String zone = null;

        private AgencyAndId route = null;

        private FareContext fareContext;

        private int numBoardings = 0;

        private boolean alightedLocal = false;

        private boolean everBoarded = false;

        private Vertex previousStop;

        private long lastAlightedTime;

        private Map<Object, Object> extensions;

        private boolean extensionsModified = false;

        private boolean created = false;

        private Editor() {

        }

        private Editor(StateData state) {
            this.trip = state.trip;
            this.tripId = state.tripId;
            this.walkDistance = state.walkDistance;
            this.zone = state.zone;
            this.route = state.route;
            this.fareContext = state.fareContext;
            this.numBoardings = state.numBoardings;
            this.alightedLocal = state.alightedLocal;
            this.everBoarded = state.everBoarded;
            this.previousStop = state.previousStop;
            this.lastAlightedTime = state.lastAlightedTime;
            this.extensions = state.extensions;
            this.extensionsModified = false;
        }

        public StateData createData() {
            /**
             * Why can a state builder only build used once? To avoid copying the extension map
             * repeatedly, we keep the original map around until modification. The trick is that if
             * you modify an extension, use the builder to create a state, and then modify another
             * extension, your modification will currently apply to the previously created state as
             * well. We could get around this by more aggressively copying the extensions map, but I
             * think the performance benefit we'll get from avoiding this will out-weight the loss
             * of convenience for an infrequent use-case.
             */
            if (created)
                throw new IllegalStateException("a state builder can only be used once");
            created = true;
            return new StateData(this);
        }

        public State createState() {
            return new State(time, createData());
        }

        public void setTime(long time) {
            this.time = time;
        }

        /**
         * Increment the time by the specified number of seconds.
         * 
         * @param numOfSeconds
         */
        public void incrementTimeInSeconds(int numOfSeconds) {
            this.time += numOfSeconds * 1000;
        }

        public void setTrip(int trip) {
            this.trip = trip;
        }

        public void setTripId(AgencyAndId tripId) {
            this.tripId = tripId;
        }

        public void setWalkDistance(double walkDistance) {
            this.walkDistance = walkDistance;
        }

        public void incrementWalkDistance(double walkDistance) {
            this.walkDistance += walkDistance;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public void setRoute(AgencyAndId route) {
            this.route = route;
        }

        public void setFareContext(FareContext fareContext) {
            this.fareContext = fareContext;
        }

        public void setNumBoardings(int numBoardings) {
            this.numBoardings = numBoardings;
        }

        public void incrementNumBoardings() {
            this.numBoardings++;
        }

        public void setAlightedLocal(boolean alightedLocal) {
            this.alightedLocal = alightedLocal;
        }

        public void setEverBoarded(boolean everBoarded) {
            this.everBoarded = everBoarded;
        }

        public void setPreviousStop(Vertex previousStop) {
            this.previousStop = previousStop;
        }

        public void setLastAlightedTime(long lastAlightedTime) {
            this.lastAlightedTime = lastAlightedTime;
        }

        public void setExtensionsModified(boolean extensionsModified) {
            this.extensionsModified = extensionsModified;
        }

        /**
         * Determine if a particular extension parameter is present for the specified key.
         * 
         * @param key
         * @return
         */
        public boolean containsExtension(Object key) {
            return extensions.containsKey(key);
        }

        /**
         * Get the extension parameter with the specified key.
         * 
         * @param <T>
         * @param key
         * @return
         */
        @SuppressWarnings("unchecked")
        public <T> T getExtension(Object key) {
            return (T) extensions.get(key);
        }

        /**
         * Add an extension parameter with the specified key. Extensions allow you to add arbitrary
         * traversal options.
         * 
         * @param key
         * @param value
         */
        public void putExtension(Object key, Object value) {

            if (!extensionsModified) {
                extensions = new HashMap<Object, Object>(extensions);
                extensionsModified = true;
            }

            extensions.put(key, value);
        }
    }
}

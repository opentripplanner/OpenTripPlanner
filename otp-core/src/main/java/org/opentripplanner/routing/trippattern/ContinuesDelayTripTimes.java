package org.opentripplanner.routing.trippattern;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public class ContinuesDelayTripTimes extends DelegatingTripTimes {

    private static final Logger LOG = LoggerFactory.getLogger(ContinuesDelayTripTimes.class);

    private NavigableMap<Integer, Integer> delayMap;

    public ContinuesDelayTripTimes(ScheduledTripTimes base) {
        super(base);
        delayMap = new TreeMap<Integer, Integer>();
    }

    @Override
    public int getDepartureTime(int hop) {
        int delayOrStatus = getDelayOrStatus(findDelayIndex(hop, false));
        if(delayOrStatus < 0){
            return delayOrStatus;
        }
        return super.getDepartureTime(hop) + delayOrStatus;
    }

    @Override
    public int getArrivalTime(int hop) {
        int delayOrStatus = getDelayOrStatus(findDelayIndex(hop, true));
        if(delayOrStatus < 0){
            return delayOrStatus;
        }
        return super.getArrivalTime(hop) + delayOrStatus;
    }

    private int getDelayOrStatus(int delayIndex){
        Map.Entry<Integer, Integer> lastDelay = delayMap.floorEntry(delayIndex);
        if (lastDelay == null){
            return TripTimes.PASSED;
        }
//        if(lastDelay.getValue() < 0){
//            return lastDelay.getValue();
//        }
        return lastDelay.getValue();
    }

    private int findDelayIndex(int hop, boolean isArrival){
        int departureIndex = hop * 2;
        if(isArrival){
            return departureIndex + 1;
        }
        return departureIndex;
    }

    public void insertUpdate(int hop, boolean isArrival,Update update){
        if(!update.getTripId().equals(this.getTrip().getId())){
            throw new IllegalArgumentException("update trip id (" + update.getTripId() +
                ") dose not match this time trip id (" + this.getTrip().getId() + ")");
        }
        int delayIndex = findDelayIndex(hop, isArrival);
        switch (update.status) {
            case PASSED:
                delayMap.put(delayIndex, TripTimes.PASSED);
                break;
            case CANCEL:
                delayMap.put(delayIndex, TripTimes.CANCELED);
                break;
            case UNKNOWN:
            case PLANNED:
                delayMap.put(delayIndex, 0);
                break;
            case ARRIVED:
            case PREDICTION:
                Integer arriveDelay = update.getArriveDelay();
                Integer departDelay = update.getDepartDelay();
                if(!update.hasDelay()){
                    arriveDelay = findDelay(update.getArrive(), hop, true);
                    departDelay = findDelay(update.getDepart(), hop, false);
                }
                if(arriveDelay != null){
                    delayMap.put(delayIndex, arriveDelay);
                }
                if(departDelay != null){
                    delayMap.put(delayIndex + 1, departDelay);
                }

                break;
        }
    }

    private Integer findDelay(int time, int hop, boolean isArrival){
        if(isArrival){
            return time - base.getArrivalTime(hop);
        }else {
            return time - base.getDepartureTime(hop + 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContinuesDelayTripTimes " +
                "(hop)<floorKey,floorValue>,departureTime[originalDepartureTime]-->arrivalTime[originalArrivalTime]")
        .append(System.getProperty("line.separator"));
        for (int i = 0; i < getNumHops(); i++) {
            int lastKey = -1, lastValue = -1;
            Map.Entry<Integer, Integer> lastDelay = delayMap.floorEntry(findDelayIndex(i, false));
            if(lastDelay != null){
                lastKey = lastDelay.getKey();
                lastValue = lastDelay.getValue();
            }
            int thisDepartureTime = getDepartureTime(i);
            int baseDepartureTime = super.getDepartureTime(i);
            int thisArrivalTime = getArrivalTime(i);
            int baseArrivalTime = super.getArrivalTime(i);
            String s = String.format("(%d)<%d,%d>,%s[%s]-->%s[%s]", i, lastKey, lastValue,
                    formatSeconds(thisDepartureTime),formatSeconds(baseDepartureTime),
                    formatSeconds(thisArrivalTime),formatSeconds(baseArrivalTime));
            sb.append(s);
        }
        return sb.toString();
    }
}

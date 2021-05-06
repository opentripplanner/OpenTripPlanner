package org.opentripplanner.netex.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopTime;


/**
 * Keep a list of stop times and the stopTimes by the timetabled-passing-time id.
 */
class StopTimesMapperResult {

    /**
     *  A ordered list of scheduled stop point ids for all timetable stop points in the
     *  order of the time-table-passing-times.
     */
    List<String> scheduledStopPointIds;

    /**
     * stopTimes by the timetabled-passing-time id
     */
    final Map<String, StopTime> stopTimeByNetexId = new HashMap<>();

    final List<StopTime> stopTimes = new ArrayList<>();


    void addStopTime(String timetabledPassingTimeId, StopTime stopTime) {
        stopTimeByNetexId.put(timetabledPassingTimeId, stopTime);
        stopTimes.add(stopTime);
    }

    void setScheduledStopPointIds(List<String> scheduledStopPointIds) {
        this.scheduledStopPointIds = scheduledStopPointIds;
    }
}

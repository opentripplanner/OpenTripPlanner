package org.opentripplanner.util.monitoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.util.MapUtils;

/**
 * This supports the monitoring of various system properties, such as free memory.
 * 
 * Think of it like a logger, except that it can be read from inside the system and it supports
 * tracking max values as well as a list of notes.  The use pattern, when monitoring is expensive,
 * is to check isMonitoring before computing anything.
 * 
 * TODO: allow registering special case monitoring for complex cases like long queries.
 * 
 * @author novalis
 * 
 */
public class MonitoringStore {

    private HashSet<String> monitoring = new HashSet<String>();

    private HashMap<String, Long> longs = new HashMap<String, Long>();

    private HashMap<String, List<String>> notes = new HashMap<String, List<String>>();

    public void addNote(String k, String v) {
        if (!monitoring.contains(k))
            return;
        MapUtils.addToMapList(notes, k, v);
    }

    public void clearNotes(String k) {
        notes.remove(k);
    }

    public Long getLong(String k) {
        return longs.get(k);
    }

    public List<String> getNotes(String k) {
        return notes.get(k);
    }

    public boolean isMonitoring(String k) {
        return monitoring.contains(k);
    }

    public void monitor(String k) {
        monitoring.add(k);
    }

    public void setLong(String k, long v) {
        if (!monitoring.contains(k))
            return;
        longs.put(k, v);
    }

    public synchronized void setLongMax(String k, long v) {
        if (!monitoring.contains(k))
            return;
        longs.put(k, v);
    }

    public void stopMonitoring(String k) {
        monitoring.remove(k);
    }

    public void setMonitoring(String key, Boolean on) {
        if (on) {
            monitoring.add(key);
        } else {
            monitoring.remove(key);
        }
    }
}

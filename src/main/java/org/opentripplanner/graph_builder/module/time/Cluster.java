package org.opentripplanner.graph_builder.module.time;

import java.util.ArrayList;
import java.util.Arrays;

public class Cluster implements Comparable<Cluster> {

    private int id;
    private TimeTable[] timetable;
    private EdgeData[] edges;

    public int getid() {
        return id;
    }

    public void setid(int id) {
        this.id = id;
    }

    public TimeTable[] gettimetable() {
        return timetable;
    }

    public ArrayList<TimeTable> gettimetableas() {
        ArrayList<TimeTable> s = new ArrayList<>(Arrays.asList(timetable));
        return s;
    }

    public void settimetable(TimeTable[] timetable) {
        this.timetable = timetable;
    }

    public EdgeData[] getedges() {
        return edges;
    }

    public void setwdges(EdgeData[] edges) {
        this.edges = edges;
    }

    @Override
    public int compareTo(Cluster o) {
        return this.getid() - o.getid();
    }
}
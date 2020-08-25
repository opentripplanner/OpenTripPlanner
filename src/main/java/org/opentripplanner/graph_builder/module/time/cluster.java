package org.opentripplanner.graph_builder.module.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class cluster implements Comparable<cluster> {

    private int id;
    private org.opentripplanner.graph_builder.module.time.timetable[] timetable;
    private edgedata[] edges;

    public int getid() {
        return id;
    }

    public void setid(int id) {
        this.id = id;
    }

    public timetable[] gettimetable() {
        return timetable;
    }
    public ArrayList<timetable>gettimetableas(){
    List<timetable> s= Arrays.asList(timetable);
     return (ArrayList<org.opentripplanner.graph_builder.module.time.timetable>) s;
    }

    public void settimetable(timetable[] timetable) {
        this.timetable = timetable;
    }

    public edgedata[] getedges() {
        return edges;
    }

    public void setwdges(edgedata[] edges) {
        this.edges = edges;
    }

    @Override
    public int compareTo(cluster o) {
        return this.getid()- o.getid();
    }
}
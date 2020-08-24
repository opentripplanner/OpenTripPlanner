package org.opentripplanner.graph_builder.module.time;

public class cluster {

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

    public void settimetable(timetable[] timetable) {
        this.timetable = timetable;
    }

    public edgedata[] getedges() {
        return edges;
    }

    public void setwdges(edgedata[] edges) {
        this.edges = edges;
    }
}
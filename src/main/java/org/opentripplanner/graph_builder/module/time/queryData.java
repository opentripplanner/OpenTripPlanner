package org.opentripplanner.graph_builder.module.time;

public class queryData {
    private int day;
    private int time;

    public queryData(int day, int time) {
        this.day = day;
        this.time = time;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}

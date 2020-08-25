package org.opentripplanner.graph_builder.module.time;

public class queryData implements Comparable<timetable> {
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

    @Override
    public int compareTo(timetable o) {
        if (this.getDay()!= o.getDaynuiber())
            return this.getDay() -o.getDaynuiber();
        if( this.getTime()<o.getStarttime())
            return -1;
        if( this.getTime()<o.getEndtime())
            return 0;
        return 1;

    }
}

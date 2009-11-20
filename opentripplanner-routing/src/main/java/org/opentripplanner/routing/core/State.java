package org.opentripplanner.routing.core;

import java.util.Date;

public class State {

    private long _time;
    private int curPattern = -1;
    private boolean justBoarded = false;
    
    public boolean getJustBoarded() {
        return justBoarded;
    }

    public void setJustBoarded(boolean justBoarded) {
        this.justBoarded = justBoarded;
    }

    public State() {
        this(System.currentTimeMillis());
    }

    public State(long time) {
        _time = time;
    }    

    public State(long time, int pattern) {
        _time = time;
        curPattern = pattern;
    }

    
    public long getTime() {
        return _time;
    }

    public void incrementTimeInSeconds(int numOfSeconds) {
        _time += numOfSeconds * 1000;
    }

    public State clone() {
        State ret = new State(_time, curPattern);
        return ret;
    }

    public String toString() {
        return "<State " + new Date(_time) + "," + curPattern + ">";
    }

    public void setPattern(int curPattern) {
        this.curPattern = curPattern;
    }

    public int getPattern() {
        return curPattern;
    }

}
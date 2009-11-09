package org.opentripplanner.routing.core;

import java.util.Date;

public class State {

    private long _time;

    public State() {
        this(System.currentTimeMillis());
    }

    public State(long time) {
        _time = time;
    }

    public long getTime() {
        return _time;
    }

    public void incrementTimeInSeconds(int numOfSeconds) {
        _time += numOfSeconds * 1000;
    }

    public State clone() {
        State ret = new State(_time);
        return ret;
    }

    public String toString() {
        return "<State " + new Date(_time) + ">";
    }

}
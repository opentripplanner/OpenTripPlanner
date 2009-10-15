package org.opentripplanner.jags.core;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class State {
	public GregorianCalendar time;
	public TimeZone timezone;
	public boolean justTransfered = false;
	
	public State() {
		this.time = new GregorianCalendar();
	}
	
	public State(GregorianCalendar time) {
		this.time = time;
	}
	
    public State clone() {
        State ret = new State();
        ret.time = (GregorianCalendar)time.clone();
        return ret;
    }
    
    public String toString() {
    	return "<State "+time.getTime()+">";
    }
   
}
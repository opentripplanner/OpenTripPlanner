package org.opentripplanner.jags.gtfs.types;

public class GTFSBoolean {
	public boolean val;
	
	public GTFSBoolean(String in){
		val = in.equals("1");
	}
	
	public String toString() {
		if(val) {
			return "1";
		} else {
			return "0";
		}
	}
}

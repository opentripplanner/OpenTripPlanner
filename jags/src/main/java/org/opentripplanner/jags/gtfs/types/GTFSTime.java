package org.opentripplanner.jags.gtfs.types;

public class GTFSTime {
	int hour;
	int minute;
	int second;
	int time;
	
	public GTFSTime(String in) {
		String[] components = in.split(":");
		hour = Integer.parseInt( components[0] );
		minute = Integer.parseInt( components[1] );
		second = Integer.parseInt( components[2] );
		time = hour*3600+minute*60+second;
	}
	
	public int getSecondsSinceMidnight() {
		return hour*3600+minute*60+second;
	}
	
	public String toString() {
		return hour+":"+minute+":"+second;
	}
}

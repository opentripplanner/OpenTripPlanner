package org.opentripplanner.jags.gtfs.types;

public class GTFSTime {
	int time;
	
	public GTFSTime() {
		
	}
	
	public GTFSTime(String in) {
		String[] components = in.split(":");
		int hour = Integer.parseInt( components[0] );
		int minute = Integer.parseInt( components[1] );
		int second = Integer.parseInt( components[2] );
		time = hour*3600+minute*60+second;
	}
	
	public int getSecondsSinceMidnight() {
		return time;
	}
	
	public String toString() {
		return time/3600+":"+(time%3600)/60+":"+time%60;
	}
	
	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}
}

package org.opentripplanner.jags.gtfs.types;

public class GTFSDate {
	
	public int year;
	public int month;
	public int day;
	
	public GTFSDate(String in) {
		year  = Integer.parseInt( in.substring(0,4) );
		month = Integer.parseInt( in.substring(4,6) );
		day   = Integer.parseInt( in.substring(6,8) );
	}
	
	public GTFSDate(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public String toString() {
		return year+"."+month+"."+day;
	}
	
	public boolean before(GTFSDate date) {
		if( this.year < date.year ) {
			return true;
		} else if( this.year == date.year && this.month < date.month ) {
			return true;
		} else if( this.year == date.year && this.month == date.month && this.day < date.day ) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean equals(GTFSDate date) {
		return this.year==date.year && this.month==date.month && this.day==date.day;
	}
	
	public boolean equals(Object obj) {
		return equals((GTFSDate)obj);
	}
	
	public boolean after(GTFSDate date) {
		if( this.before(date) || this.equals(date) ) {
			return false;
		} else {
			return true;
		}
	}
	
	public int hashCode() {
		return Integer.valueOf(this.year+this.month+this.day).hashCode();
	}

}

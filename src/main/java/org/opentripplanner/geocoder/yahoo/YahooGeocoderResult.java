package org.opentripplanner.geocoder.yahoo;

public class YahooGeocoderResult {
	
	private String latitude;
	private String longitude;
	
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	
	public double getLatDouble() {
		return Double.parseDouble(latitude);
	}
	
	public double getLngDouble() {
		return Double.parseDouble(longitude);
	}
	
	private String line1;
	private String line2;

	public String getLine1() {
		return line1;
	}
	public void setLine1(String line1) {
		this.line1 = line1;
	}
	public String getLine2() {
		return line2;
	}
	public void setLine2(String line2) {
		this.line2 = line2;
	}
}

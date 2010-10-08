package org.opentripplanner.geocoder.google;

import java.util.List;

public class AddressComponent {
	
	private String long_name;
	private String short_name;
	private List<String> types;
	
	public String getLong_name() {
		return long_name;
	}
	public void setLong_name(String longName) {
		long_name = longName;
	}
	public String getShort_name() {
		return short_name;
	}
	public void setShort_name(String shortName) {
		short_name = shortName;
	}
	public List<String> getTypes() {
		return types;
	}
	public void setTypes(List<String> types) {
		this.types = types;
	}
}

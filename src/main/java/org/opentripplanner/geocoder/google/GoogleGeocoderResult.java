package org.opentripplanner.geocoder.google;

public class GoogleGeocoderResult {
	private Geometry geometry;
	private String formatted_address;

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public String getFormatted_address() {
		return formatted_address;
	}

	public void setFormatted_address(String formattedAddress) {
		formatted_address = formattedAddress;
	}

}

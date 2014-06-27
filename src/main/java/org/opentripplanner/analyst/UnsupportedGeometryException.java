package org.opentripplanner.analyst;

public class UnsupportedGeometryException extends Exception {

	public String message;

	public UnsupportedGeometryException(String message) {
		this.message = message;
	}

}

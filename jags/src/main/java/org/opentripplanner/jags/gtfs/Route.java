package org.opentripplanner.jags.gtfs;

import java.lang.reflect.InvocationTargetException;

import org.opentripplanner.jags.core.TransportationMode;


public class Route extends Record {
	public String route_id;
	public String agency_id;
	public String route_short_name;
	public String route_long_name;
	public String route_desc;
	public String route_type;
	public String route_url;
	public String route_color;
	public String route_text_color;

	// null constructor for Hibernate
	public Route() {}
	
	Route(TableHeader header, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		super(header, record);
	}
	
	public String toString() {
		return "<Route "+route_id+">";
	}
	
	public String getRoute_id() {
		return route_id;
	}

	public void setRoute_id(String routeId) {
		route_id = routeId;
	}

	public String getAgency_id() {
		return agency_id;
	}

	public void setAgency_id(String agencyId) {
		agency_id = agencyId;
	}

	public String getRoute_short_name() {
		return route_short_name;
	}

	public void setRoute_short_name(String routeShortName) {
		route_short_name = routeShortName;
	}

	public String getRoute_long_name() {
		return route_long_name;
	}

	public void setRoute_long_name(String routeLongName) {
		route_long_name = routeLongName;
	}

	public String getRoute_desc() {
		return route_desc;
	}

	public void setRoute_desc(String routeDesc) {
		route_desc = routeDesc;
	}

	public String getRoute_type() {
		return route_type;
	}

	public void setRoute_type(String routeType) {
		route_type = routeType;
	}

	public String getRoute_url() {
		return route_url;
	}

	public void setRoute_url(String routeUrl) {
		route_url = routeUrl;
	}

	public String getRoute_color() {
		return route_color;
	}

	public void setRoute_color(String routeColor) {
		route_color = routeColor;
	}

	public String getRoute_text_color() {
		return route_text_color;
	}

	public void setRoute_text_color(String routeTextColor) {
		route_text_color = routeTextColor;
	}

	public TransportationMode getTransportationMode() {
		int i = Integer.parseInt(route_type);
		return TransportationMode.values()[i];
	}

	public String getName() {
		if (route_short_name != null && !route_short_name.equals(null)) {
			return route_short_name;
		}
		return route_long_name;
	}
	
}

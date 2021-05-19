package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class ParkAndRideOpeningHoursUnparsed implements DataImportIssue {

    public static final String FMT = "Park and ride '%s' (%s) has an invalid opening_hours value, it will always be open: %s";
    public static final String HTMLFMT = "Park and ride <a href='%s'>'%s' (%s)</a> has an invalid opening_hours value, it will always be open: <code>%s</code>\"";

    private final String name;
    private final OSMWithTags entity;
    private final String openingHours;

    public ParkAndRideOpeningHoursUnparsed(String name, OSMWithTags entity, String openingHours){
    	this.name = name;
    	this.entity = entity;
    	this.openingHours = openingHours;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, name, entity, openingHours);
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, entity.getOpenStreetMapLink(), name, entity, openingHours);
    }

}

package org.opentripplanner.graph_builder.annotation;

public class StreetCarSpeedZero extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 6872784791854835184L;

    public static final String FMT = "Way %s has car speed zero";
    public static final String HTMLFMT = "Way <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a> has car speed zero";
    
    final long wayId;
    
    public StreetCarSpeedZero(long wayId){
    	this.wayId = wayId;
    }

    @Override
    public String getHTMLMessage() {
        if (wayId > 0 ) {
            return String.format(HTMLFMT, wayId, wayId);
            // If way is lower then 0 it means it is temporary ID and so useless to link to OSM
        } else {
            return getMessage();
        }
    }

    @Override
    public String getMessage() {
        return String.format(FMT, wayId);
    }
}

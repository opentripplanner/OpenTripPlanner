package org.opentripplanner.graph_builder.annotation;

public class ConflictingBikeTags extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Conflicting tags bicycle:[yes|designated] and cycleway: " +
    		"dismount on way %s, assuming dismount";
    public static final String HTMLFMT = "Conflicting tags bicycle:[yes|designated] and cycleway: " +
        "dismount on way <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a>, assuming dismount";
    
    final long wayId;
    
    public ConflictingBikeTags(long wayId){
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

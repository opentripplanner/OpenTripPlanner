package org.opentripplanner.graph_builder.annotation;

public class TurnRestrictionException extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Turn restriction with bicycle exception at node %s from %s";

    public static final String HTMLFMT = "Turn restriction with bicycle exception at node <a href=\"http://www.openstreetmap.org/node/%d\">\"%d\"</a> from <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a>";
    
    final long nodeId, wayId;
    
    public TurnRestrictionException(long nodeId, long wayId){
    	this.nodeId = nodeId;
    	this.wayId = wayId;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, nodeId, nodeId, wayId, wayId);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, nodeId, wayId);
    }

}

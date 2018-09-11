package org.opentripplanner.graph_builder.annotation;

public class TurnRestrictionUnknown extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Invalid turn restriction tag %s in turn restriction %d";

    public static final String HTMLFMT = "Invalid turn restriction tag %s in  <a href=\"http://www.openstreetmap.org/relation/%d\">\"%d\"</a>";
    
    final String tagval;
    final long relationId;
    
    public TurnRestrictionUnknown(long relationId, String tagval){
        this.relationId = relationId;
    	this.tagval = tagval;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(FMT, tagval, relationId, relationId);
    }

    @Override
    public String getMessage() {
        return String.format(FMT, tagval, relationId);
    }

}

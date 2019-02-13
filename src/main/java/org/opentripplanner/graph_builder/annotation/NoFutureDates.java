package org.opentripplanner.graph_builder.annotation;

public class NoFutureDates extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Agency %s has no calendar dates which are after today; " +
    		"no trips will be plannable on this agency";
    
    final String agency;
    
    public NoFutureDates(String agency){
    	this.agency = agency;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, agency);
    }

}

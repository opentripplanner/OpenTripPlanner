package org.opentripplanner.graph_builder.annotation;

public class Graphwide extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    String message;
    
    public Graphwide(String message){
    	this.message = message;
    }

    @Override
    public String getMessage() {
        return String.format("graph-wide: " + message);
    }
    
}
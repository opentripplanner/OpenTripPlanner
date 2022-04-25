package org.opentripplanner.graph_builder.module.osm.exception;

public class OSMProcessingException extends RuntimeException {

  public OSMProcessingException() {
    super();
  }

  public OSMProcessingException(String message) {
    super(message);
  }

  public OSMProcessingException(Throwable cause) {
    super(cause);
  }
}

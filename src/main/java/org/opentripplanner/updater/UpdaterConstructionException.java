package org.opentripplanner.updater;

public class UpdaterConstructionException extends RuntimeException {

  public UpdaterConstructionException(Throwable cause) {
    super(cause);
  }

  public UpdaterConstructionException(String message) {
    super(message);
  }
}

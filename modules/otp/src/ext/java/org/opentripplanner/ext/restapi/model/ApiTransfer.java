package org.opentripplanner.ext.restapi.model;

/** Represents a transfer from a stop */
public class ApiTransfer {

  /** The stop we are connecting to */
  public String toStopId;

  /** the on-street distance of the transfer (meters) */
  public double distance;
}

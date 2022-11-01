package org.opentripplanner.ext.traveltime;

/**
 * The default TZ data we keep for each sample: Weighted Time and Walk Distance
 * <p>
 * For now we keep all possible values in the vector; we may want to remove the values that will not
 * be used in the process (for example # of boardings). Currently, the filtering is done afterwards,
 * it may be faster and surely less memory-intensive to do the filtering when processing.
 *
 * @author laurent
 */
public class WTWD {

  /* Total weight */
  public double w;

  // TODO Add generalized cost

  /* Weighted sum of time in seconds */
  public double wTime;

  /* Weighted sum of walk distance in meters */
  public double wWalkDist;

  /* Minimum off-road distance to any sample */
  public double d;

  @Override
  public String toString() {
    return String.format("[t/w=%f,w=%f,d=%f]", wTime / w, w, d);
  }
}

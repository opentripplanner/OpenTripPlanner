package org.opentripplanner.api.error;

import java.util.List;
import org.opentripplanner.api.common.Message;

/** This API response element represents an error in trip planning. */
public class PlannerError {

  public final int id;
  public final String msg;
  public final Message message;
  public List<String> missing = null;

  /** An error where no path has been found, but no points are missing */
  public PlannerError(Message msg) {
    this.message = msg;
    this.msg = msg.get();
    this.id = msg.getId();
  }

  /**
   * @return the list of point names which cannot be found (from, to, intermediate.n)
   */
  public List<String> getMissing() {
    return missing;
  }

  /**
   * @param missing the list of point names which cannot be found (from, to, intermediate.n)
   */
  public void setMissing(List<String> missing) {
    this.missing = missing;
  }
}

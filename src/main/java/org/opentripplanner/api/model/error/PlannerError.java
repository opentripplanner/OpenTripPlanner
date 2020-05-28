package org.opentripplanner.api.model.error;

import org.opentripplanner.api.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** This API response element represents an error in trip planning. */
public class PlannerError {

    private static final Logger LOG = LoggerFactory.getLogger(PlannerError.class);

    public int    id;
    public String msg;
    public Message message;
    private List<String> missing = null;
    private boolean noPath = false;

    /** An error where no path has been found, but no points are missing */
    public PlannerError() {
        noPath = true;
    }

    public void setMsg(Message msg) {
        this.msg = msg.get();
        this.id  = msg.getId();
    }

    /**
     * @param missing the list of point names which cannot be found (from, to, intermediate.n)
     */
    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    /**
     * @return the list of point names which cannot be found (from, to, intermediate.n)
     */
    public List<String> getMissing() {
        return missing;
    }
}

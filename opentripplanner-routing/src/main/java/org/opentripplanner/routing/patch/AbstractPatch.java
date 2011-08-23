package org.opentripplanner.routing.patch;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.opentripplanner.routing.core.TraverseOptions;

public abstract class AbstractPatch implements Patch {
    protected String id;

    protected Alert alert;

    private static final long serialVersionUID = 1371103825857750564L;

    private List<TimePeriod> timePeriods = new ArrayList<TimePeriod>();

    @Override
    public boolean activeDuring(TraverseOptions options, long start, long end) {
        for (TimePeriod period : timePeriods) {
            if (!(end <= period.startTime || start >= period.endTime)) {
                return true;
            }
        }
        return false;
    }
    
    @XmlElement
    @Override
    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    @XmlElement
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }

    private void writeObject(ObjectOutputStream os) throws IOException {
        if (timePeriods instanceof ArrayList<?>) {
            ((ArrayList<TimePeriod>) timePeriods).trimToSize();
        }
        os.defaultWriteObject();
    }

    public void addTimePeriod(long start, long end) {
        timePeriods.add(new TimePeriod(start, end));
    }
    
    public void setTimePeriods(List<TimePeriod> periods) {
        timePeriods = periods;
    }
}
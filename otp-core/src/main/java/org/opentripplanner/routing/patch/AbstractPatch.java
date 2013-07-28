/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.patch;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.opentripplanner.routing.core.RoutingRequest;

public abstract class AbstractPatch implements Patch {
    private static final long serialVersionUID = 1371103825857750564L;

    protected String id;

    protected Alert alert;

    protected List<TimePeriod> timePeriods = new ArrayList<TimePeriod>();
    protected List<TimePeriod> displayTimePeriods = new ArrayList<TimePeriod>();

    @Override
    public boolean activeDuring(RoutingRequest options, long start, long end) {
        for (TimePeriod period : timePeriods) {
            if (!(end <= period.startTime || start >= period.endTime)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean displayDuring(RoutingRequest options, long start, long end) {
        for (TimePeriod period : displayTimePeriods) {
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

    public void addDisplayTimePeriod(long start, long end) {
        displayTimePeriods.add(new TimePeriod(start, end));
    }
    
    public void setDisplayTimePeriods(List<TimePeriod> periods) {
        displayTimePeriods = periods;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AbstractPatch)) {
            return false;
        }
        AbstractPatch other = (AbstractPatch) o;
        return id.equals(other.id) && timePeriods.equals(other.timePeriods) && displayTimePeriods.equals(other.displayTimePeriods);
    }
}

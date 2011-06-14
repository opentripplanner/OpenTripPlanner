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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseOptions;

@XmlType
@XmlTransient
public abstract class Patch implements Serializable {
	private static final long serialVersionUID = 778531395626383517L;

	private long endTime;
	private long startTime;
	private int startTimeOfDay = 0; //by default, the whole day
	private int endTimeOfDay = 86400;
	protected String notes;

	private String id;

	@XmlElement
	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	@XmlElement
	public int getEndTimeOfDay() {
		return endTimeOfDay;
	}

	public void setEndTimeOfDay(int endTimeOfDay) {
		this.endTimeOfDay = endTimeOfDay;
	}

	@XmlElement
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	@XmlElement
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	@XmlElement
	public int getStartTimeOfDay() {
		return startTimeOfDay;
	}

	public void setStartTimeOfDay(int startTimeOfDay) {
		this.startTimeOfDay = startTimeOfDay;
	}

	public boolean activeDuring(TraverseOptions options, long start, long end) {
		if (end < startTime || start >= endTime) {
			return false;
		}

		int eventStart = -1;
		int eventEnd = -1;
		for (ServiceDay sd : options.serviceDays) {
            eventStart = sd.secondsSinceMidnight(start);
            eventEnd = sd.secondsSinceMidnight(end);
            if (eventStart >= 0 && eventStart < 86400) {
            	break;
            }
		}
		return eventEnd >= startTimeOfDay && eventStart < endTimeOfDay;
	}

	public static State filterTraverseResultChain(State result, 
			TraverseResultFilter traverseResultFilter) {
		State out = null;
		for (State old = result; old != null; old = old.getNextResult()) {
			State filtered = traverseResultFilter.filter(old);
			if (out == null) {
				out = filtered;
			} else {
				filtered.addToExistingResultChain(out);
			}
		}
		return out;
	}

	@XmlElement(required = true)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public abstract void apply(Graph graph);

	public abstract void remove(Graph graph);

	public abstract void filterTraverseResult(StateEditor result);

	public int hashCode() {
		return id.hashCode();
	}

	public boolean equals(Object o) {
		return (o instanceof Patch && ((Patch) o).getId().equals(id));
	}
}

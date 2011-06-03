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

import org.opentripplanner.routing.core.TraverseResult;

public abstract class AbstractPatch implements Patch, Serializable {
	private static final long serialVersionUID = 778531395626383517L;

	private long endTime;
	private long startTime;
	private int startTimeOfDay;
	private int endTimeOfDay;
	protected String notes;

	AbstractPatch(long startTime, long endTime, int startTimeOfDay,
			int endTimeOfDay, String notes) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.startTimeOfDay = startTimeOfDay;
		this.endTimeOfDay = endTimeOfDay;
		this.notes = notes;
	}

	public long getEndTime() {
		return endTime;
	}

	public int getEndTimeOfDay() {
		return endTimeOfDay;
	}

	@Override
	public String getNotes() {
		return notes;
	}

	public long getStartTime() {
		return startTime;
	}

	public int getStartTimeOfDay() {
		return startTimeOfDay;
	}

	public boolean activeDuring(long start, long end) {
		if (end < startTime || start >= endTime) {
			return false;
		}
		start /= 1000;
		end /= 1000;
		long eventStart = start % 86400;
		long eventEnd = end % 86400;
		return eventEnd >= startTimeOfDay && eventStart < endTimeOfDay; 
	}
	
	public static TraverseResult filterTraverseResultChain(TraverseResult result,
			TraverseResultFilter traverseResultFilter) {
		TraverseResult out = null;
		for (TraverseResult old = result; old != null; old = old.getNextResult()) {
			TraverseResult filtered = traverseResultFilter.filter(old);
			if (out == null) {
				out = filtered;
			} else {
				filtered.addToExistingResultChain(out);
			}
		}
		return out;
	}
}

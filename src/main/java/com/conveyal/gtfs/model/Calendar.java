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

package com.conveyal.gtfs.model;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.error.DuplicateKeyError;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class Calendar extends Entity {

    public Service service;
    public int monday;
    public int tuesday;
    public int wednesday;
    public int thursday;
    public int friday;
    public int saturday;
    public int sunday;
    public int start_date;
    public int end_date;

    public static class Loader extends Entity.Loader<Calendar> {

        public Loader(GTFSFeed feed) {
            super(feed, "calendars");
        }

        @Override
        public void loadOneRow() throws IOException {

            /* Calendars and Fares are special: they are stored as joined tables rather than simple maps. */
            String service_id = getStringField("service_id", true); // TODO service_id can reference either calendar or calendar_dates.
            Service service = feed.getOrCreateService(service_id);
            if (service.calendar != null) {
                feed.errors.add(new DuplicateKeyError(tableName, row, "service_id"));
            } else {
                Calendar c = new Calendar();
                c.service = service;
                c.monday = getIntField("monday", true, 0, 1);
                c.tuesday = getIntField("tuesday", true, 0, 1);
                c.wednesday = getIntField("wednesday", true, 0, 1);
                c.thursday = getIntField("thursday", true, 0, 1);
                c.friday = getIntField("friday", true, 0, 1);
                c.saturday = getIntField("saturday", true, 0, 1);
                c.sunday = getIntField("sunday", true, 0, 1);
                c.start_date = getIntField("start_date", true, 0, 1);
                c.end_date = getIntField("end_date", true, 0, 1);
                c.feed = feed;
                service.calendar = c;
            }

        }    
    }
    
    /**
     * An iterator over calendars from the joined service table.
     * Just wrap an iterator over services.
     */
    public static class CalendarServiceIterator implements Iterator<Calendar> {
    	private Iterator<Service> wrapped;
    	
    	public CalendarServiceIterator(Iterator<Service> services) {
    		wrapped = services;
    	}
    	
    	@Override
    	public boolean hasNext() {
    		return wrapped.hasNext();
    	}

		@Override
		public Calendar next() {
			return wrapped.next().calendar;
		}

		@Override
		public void remove() {
			wrapped.remove();
		}
    }
    
    public static class Writer extends Entity.Writer<Calendar> {
        public Writer(GTFSFeed feed) {
        	super(feed, "calendar");
        }

		@Override
		protected void writeHeaders() throws IOException {
			writer.writeRecord(new String[] {"service_id", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "start_date", "end_date"});
		}

		@Override
		protected void writeOneRow(Calendar c) throws IOException {
			writeStringField(c.service.service_id);
			writeIntField(c.monday);
			writeIntField(c.tuesday);
			writeIntField(c.wednesday);
			writeIntField(c.thursday);
			writeIntField(c.friday);
			writeIntField(c.saturday);
			writeIntField(c.sunday);
			writeIntField(c.start_date);
			writeIntField(c.end_date);
			endRecord();
		}

		@Override
		protected Iterator<Calendar> iterator() {
			return new CalendarServiceIterator(feed.services.values().iterator());
		}
        
        
    }
}

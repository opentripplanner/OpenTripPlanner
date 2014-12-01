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
import com.sun.org.apache.xerces.internal.impl.dv.xs.DateTimeDV;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Iterator;

public class CalendarDate extends Entity {

    public Service  service;
    public DateTime date;
    public int      exception_type;

    public static class Loader extends Entity.Loader<CalendarDate> {

        public Loader(GTFSFeed feed) {
            super(feed, "calendar_dates");
        }

        @Override
        public void loadOneRow() throws IOException {
            /* Calendars and Fares are special: they are stored as joined tables rather than simple maps. */
            String service_id = getStringField("service_id", true);
            Service service = feed.getOrCreateService(service_id);
            DateTime date = getDateField("date", true);
            if (service.calendar_dates.containsKey(date)) {
                feed.errors.add(new DuplicateKeyError(tableName, row, "(service_id, date)"));
            } else {
                CalendarDate cd = new CalendarDate();
                cd.service = service;
                cd.date = date;
                cd.exception_type = getIntField("exception_type", true, 0, 1);
                cd.feed = feed;
                service.calendar_dates.put(date, cd);
            }
        }
    }

    /**
     * Calendar dates are stored inside services, each of which can have multiple calendar dates.
     * So this iterator wraps an iterator over services and returns each calendar date.
     * Calendar dates can only be associated with a single service, so we need not worry much about duplicates.
     * 
     * The behavior of this iterator is undefined if the underlying map changes during iteration.
     * 
     * @author mattwigway
     */
    private static class CalendarDateServiceIterator implements Iterator<CalendarDate> {
        private Iterator<Service> services;
        private Iterator<CalendarDate> currentService;

        public CalendarDateServiceIterator(Iterator<Service> services) {
            this.services = services;
            currentService = null;
        }

        private void findNext() {

        }

        @Override
        public boolean hasNext() {
            // scan through the services
            while (currentService == null || !currentService.hasNext()) {
                if (!services.hasNext())
                    return false;

                currentService = services.next().calendar_dates.values().iterator();
            }

            return true;
        }

        @Override
        public CalendarDate next() {
            // we call hasNext to position ourselves at the next item.
            if (!hasNext())
                return null;
            else
                return currentService.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove calendar dates during iteration.");
        }

    }

    public static class Writer extends Entity.Writer<CalendarDate> {
        public Writer (GTFSFeed feed) {
            super(feed, "calendar_dates");
        }

        @Override
        protected void writeHeaders() throws IOException {
            writer.writeRecord(new String[] {"service_id", "date", "exception_type"});
        }

        @Override
        protected void writeOneRow(CalendarDate d) throws IOException {
            writeStringField(d.service.service_id);
            writeDateField(d.date);
            writeIntField(d.exception_type);
            endRecord();
        }

        @Override
        protected Iterator<CalendarDate> iterator() {
            return new CalendarDateServiceIterator(feed.services.values().iterator());
        }


    }
}

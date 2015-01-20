package com.conveyal.gtfs.model;

import com.beust.jcommander.internal.Sets;
import com.conveyal.gtfs.GTFSFeed;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.conveyal.gtfs.error.*;
import com.google.common.io.ByteStreams;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.net.URL;

/**
 * An abstract base class that represents a row in a GTFS table, e.g. a Stop, Trip, or Agency.
 * One concrete subclass is defined for each table in a GTFS feed.
 */
// TODO K is the key type for this table
public abstract class Entity implements Serializable {

    public static final int INT_MISSING = Integer.MIN_VALUE;

    /* The feed from which this entity was loaded. */
    GTFSFeed feed;

    /* A class that can produce Entities from CSV, and record errors that occur in the process. */
    // This is almost a GTFSTable... rename?
    public static abstract class Loader<E extends Entity> {

        private static final Logger LOG = LoggerFactory.getLogger(Loader.class);
        private static final Deduplicator deduplicator = new Deduplicator();

        protected final GTFSFeed feed;    // the feed into which we are loading the entities
        protected final String tableName; // name of corresponding table without .txt
        protected final Set<String> missingRequiredColumns = Sets.newHashSet();
        public boolean required = false;

        protected CsvReader reader;
        protected long      row;
        // TODO "String column" that is set before any calls to avoid passing around the column name

        public Loader(GTFSFeed feed, String tableName) {
            this.feed = feed;
            this.tableName = tableName;
        }

        /** @return whether the number actual is in the range [min, max] */
        protected boolean checkRangeInclusive(double min, double max, double actual) {
            if (actual < min || actual > max) {
                feed.errors.add(new RangeError(tableName, row, null, min, max, actual)); // TODO set column name in loader so it's available in methods
                return false;
            }
            return true;
        }

        /**
         * Fetch the value from the given column of the current row. Record an error the first time a column is
         * seen to be missing, and whenever empty values are encountered.
         * I was originally just calling getStringField from the other getXField functions as a first step to get
         * the missing-field check. But we don't want deduplication performed on strings that aren't being retained.
         * Therefore the missing-field behavior is this separate function.
         * @return null if column was missing or field is empty
         */
        private String getFieldCheckRequired(String column, boolean required) throws IOException {
            String str = reader.get(column);
            if (str == null) {
                if (!missingRequiredColumns.contains(column)) {
                    feed.errors.add(new MissingColumnError(tableName, column));
                    missingRequiredColumns.add(column);
                }
            } else if (str.isEmpty()) {
                if (required) {
                    feed.errors.add(new EmptyFieldError(tableName, row, column));
                }
                str = null;
            }
            return str;
        }

        /** @return the given column from the current row as a deduplicated String. */
        protected String getStringField(String column, boolean required) throws IOException {
            String str = getFieldCheckRequired(column, required);
            str = deduplicator.deduplicateString(str);
            return str;
        }

        protected int getIntField(String column, boolean required, int min, int max) throws IOException {
            String str = getFieldCheckRequired(column, required);
            int val = INT_MISSING;
            if (str == null) {
                val = 0; // TODO boolean emptyMeansZero (in one case this is not true)
            } else try {
                val = Integer.parseInt(str);
                checkRangeInclusive(min, max, val);
            } catch (NumberFormatException nfe) {
                feed.errors.add(new NumberParseError(tableName, row, column));
            }
            return val;
        }

        /**
         * Fetch the given column of the current row, and interpret it as a time in the format HH:MM:SS.
         * @return the time value in seconds since midnight
         */
        protected int getTimeField(String column) throws IOException {
            String str = getFieldCheckRequired(column, true); // All time fields are required fields
            int val = INT_MISSING;
            String[] fields = str.split(":");
            if (fields.length != 3) {
                feed.errors.add(new TimeParseError(tableName, row, column));
            } else {
                try {
                    int hours = Integer.parseInt(fields[0]);
                    int minutes = Integer.parseInt(fields[1]);
                    int seconds = Integer.parseInt(fields[2]);
                    checkRangeInclusive(0, 72, hours); // GTFS hours can go past midnight. Some trains run for 3 days.
                    checkRangeInclusive(0, 59, minutes);
                    checkRangeInclusive(0, 59, seconds);
                    val = (hours * 60 * 60) + minutes * 60 + seconds;
                } catch (NumberFormatException nfe) {
                    feed.errors.add(new TimeParseError(tableName, row, column));
                }
            }
            return val;
        }

        /**
         * Fetch the given column of the current row, and interpret it as a date in the format YYYYMMDD.
         * @return the date value as Joda DateTime, or null if it could not be parsed.
         */
        protected DateTime getDateField(String column, boolean required) throws IOException {
            String str = getFieldCheckRequired(column, required);
            DateTime dateTime = null;
            if (str != null) try {
                DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd");
                dateTime = formatter.parseDateTime(str);
                checkRangeInclusive(2000, 2100, dateTime.getYear());
            } catch (IllegalArgumentException iae) {
                feed.errors.add(new DateParseError(tableName, row, column));
            }
            return dateTime;
        }

        /**
         * Fetch the given column of the current row, and interpret it as a URL.
         * @return the URL, or null if the field was missing or empty.
         */
        protected URL getUrlField(String column, boolean required) throws IOException {
            String str = getFieldCheckRequired(column, required);
            URL url = null;
            if (str != null) try {
                url = new URL(str);
            } catch (MalformedURLException mue) {
                feed.errors.add(new URLParseError(tableName, row, column));
            }
            return url;
        }

        protected double getDoubleField(String column, boolean required, double min, double max) throws IOException {
            String str = getFieldCheckRequired(column, required);
            double val = Double.NaN;
            if (str != null) try {
                val = Double.parseDouble(str);
                checkRangeInclusive(min, max, val);
            } catch (NumberFormatException nfe) {
                feed.errors.add(new NumberParseError(tableName, row, column));
            }
            return val;
        }

        /**
         * Used to check referential integrity.
         * TODO Return value is not yet used, but could allow entities to point to each other directly rather than
         * using indirection through string-keyed maps.
         */
        protected <K, V> V getRefField(String column, boolean required, Map<K, V> target) throws IOException {
            String str = getFieldCheckRequired(column, required);
            V val = null;
            if (str != null) {
                val = target.get(str);
                if (val == null) {
                    feed.errors.add(new ReferentialIntegrityError(tableName, row, column, str));
                }
            }
            return val;
        }

        /** Implemented by subclasses to read one row, produce one GTFS entity, and store that entity in a map. */
        protected abstract void loadOneRow() throws IOException;

        /**
         * The main entry point into an Entity.Loader. Interprets each row of a CSV file within a zip file as a sinle
         * GTFS entity, and loads them into a table.
         *
         * @param zip the zip file from which to read a table
         */
        public void loadTable(ZipFile zip) throws IOException {
            ZipEntry entry = zip.getEntry(tableName + ".txt");
            if (entry == null) {
                /* This GTFS table did not exist in the zip. */
                if (required) {
                    feed.errors.add(new MissingTableError(tableName));
                } else {
                    LOG.info("Table {} was missing but it is not required.", tableName);
                }
                return;
            }
            LOG.info("Loading GTFS table {} from {}", tableName, entry);
            InputStream zis = zip.getInputStream(entry);
            CsvReader reader = new CsvReader(zis, ',', Charset.forName("UTF8"));
            this.reader = reader;
            reader.readHeaders();
            while (reader.readRecord()) {
                // reader.getCurrentRecord() is zero-based and does not include the header line, keep our own row count
                if (++row % 500000 == 0) {
                    LOG.info("Record number {}", human(row));
                }
                loadOneRow(); // Call subclass method to produce an entity from the current row.
            }
        }

    }

    /**
     * An output stream that cannot be closed. CSVWriters try to close their output streams when they are garbage-collected,
     * which breaks if another CSV writer is still writing to the ZIP file.
     *
     * Apache Commons has something similar but it seemed silly to import another large dependency. Eventually Guava will have this,
     * see Guava issue 1367. At that point we should switch to using Guava.
     */
    private static class UncloseableOutputStream extends FilterOutputStream {
        public UncloseableOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close () {
            // no-op
            return;
        }
    }

    /**
     * Write this entity to a CSV file. This should be subclassed in subclasses of Entity.
     * The following (abstract) methods should be overridden in a subclass:
     * 
     * writeHeaders(): write the headers to the CsvWriter writer.
     * writeRow(E): write the passed-in object to the CsvWriter writer, potentially using the write*Field methods.
     * iterator(): return an iterator over objects of this class (note that the feed is available at this.feed
     * public Writer (GTFSFeed feed): this should super to Writer(GTFSFeed feed, String tableName), with the table name
     * defined. 
     * 
     * @author mattwigway
     */
    public static abstract class Writer<E extends Entity> {
        private static final Logger LOG = LoggerFactory.getLogger(Writer.class);

        protected final GTFSFeed feed;    // the feed into which we are loading the entities
        protected final String tableName; // name of corresponding table without .txt

        protected CsvWriter writer;

        /**
         * one-based to match reader.
         */
        protected long row;

        protected Writer(GTFSFeed feed, String tableName) {
            this.feed = feed;
            this.tableName = tableName;
        }

        /**
         * Write the CSV header.
         */
        protected abstract void writeHeaders() throws IOException;

        /**
         * Write one row of the CSV from the passed-in object.
         */
        protected abstract void writeOneRow(E obj) throws IOException;

        /**
         * Get an iterator over objects of this type.
         */
        protected abstract Iterator<E> iterator();

        public void writeTable (ZipOutputStream zip) throws IOException {
            LOG.info("Writing GTFS table {}", tableName);

            ZipEntry zipEntry = new ZipEntry(tableName + ".txt");
            zip.putNextEntry(zipEntry);

            // don't let CSVWriter close the stream when it is garbage-collected
            OutputStream protectedOut = new UncloseableOutputStream(zip);
            this.writer = new CsvWriter(protectedOut, ',', Charset.forName("UTF8"));

            this.writeHeaders();

            // write rows until there are none left.
            row = 0;        	
            Iterator<E> iter = this.iterator();
            while (iter.hasNext()) {
                if (++row % 500000 == 0) {
                    LOG.info("Record number {}", human(row));
                }

                writeOneRow(iter.next());
            }

            // closing the writer closes the underlying output stream, so we don't do that.
            writer.flush();
            zip.closeEntry();

            LOG.info("Wrote {} rows", human(row));
        }

        protected void writeStringField(String str) throws IOException {
            writer.write(str);
        }

        protected void writeUrlField(URL obj) throws IOException {
            writeStringField(obj != null ? obj.toString() : "");
        }

        protected void writeDateField (DateTime d) throws IOException {
            writeStringField(String.format("%04d%02d%02d", d.getYear(), d.getMonthOfYear(), d.getDayOfMonth()));
        }

        /**
         * Take a time expressed in seconds since noon - 12h (midnight, usually) and write it in HH:MM:SS format.
         */
        protected void writeTimeField (int secsSinceMidnight) throws IOException {
            int seconds = secsSinceMidnight % 60;
            secsSinceMidnight -= seconds;
            // note that the minute and hour values are still expressed in seconds until we write it out, to avoid unnecessary division.
            int minutes = (secsSinceMidnight % 3600);
            // secsSinceMidnight now represents hours
            secsSinceMidnight -= minutes;

            // integer divide is fine as we've subtracted off remainders
            writeStringField(String.format("%02d:%02d:%02d", secsSinceMidnight / 3600, minutes / 60, seconds));
        }

        protected void writeIntField (Integer val) throws IOException {
            if (val.equals(INT_MISSING))
                writeStringField("");
            else
                writeStringField(val.toString());
        }

        /**
         * Write a double value, with precision 10^-7.
         */
        protected void writeDoubleField (double val) throws IOException {
            // control file size: don't use unnecessary precision
            // This is usually used for coordinates; one ten-millionth of a degree at the equator is 1.1cm,
            // and smaller elsewhere on earth, plenty precise enough.
            // On Jupiter, however, it's a different story.
            writeStringField(String.format("%.7f", val));
        }

        /**
         * End a row.
         * This is just a proxy to the writer, but could be used for hooks in the future.
         */
        public void endRecord () throws IOException {
            writer.endRecord();
        }
    }


    // shared code between reading and writing
    private static final String human (long n) {
        if (n >= 1000000) return String.format("%.1fM", n/1000000.0);
        if (n >= 1000) return String.format("%.1fk", n/1000.0);
        else return String.format("%d", n);
    }
}

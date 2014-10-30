package com.conveyal.gtfs.model;

import com.beust.jcommander.internal.Lists;
import com.csvreader.CsvReader;
import com.conveyal.gtfs.error.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An abstract base class that represents a row in a GTFS table, e.g. a Stop, Trip, or Agency.
 * One concrete subclass is defined for each table in a GTFS feed.
 */
// TODO K is the key type for this table
public abstract class Entity {

    /* The ID of the feed from which this entity was loaded. */
    String feedId;

    public abstract Object getKey();

    public static final int INT_MISSING = Integer.MIN_VALUE;

    /* A class that can produce Entities from CSV, and record errors that occur in the process. */
    // This is almost a GTFSTable... rename?
    public static abstract class Factory<E extends Entity> {

        private static final Logger LOG = LoggerFactory.getLogger(Factory.class);

        // TODO private static final StringDeduplicator;

        String   tableName; // name of corresponding table without .txt
        String[] requiredColumns;
        boolean required = false;

        CsvReader reader;
        long row;
        // TODO "String column" that is set before any calls to avoid passing around the column name
        List<GTFSError> errorList = Lists.newArrayList(); // TODO collapse empty field errors when the column is entirely missing

        /** @return whether the number actual is in the range [min, max] */
        protected boolean checkRangeInclusive(int min, int max, double actual) {
            if (actual < min || actual > max) {
                errorList.add(new RangeError(tableName, row, null, min, max, actual));
                return false;
            }
            return true;
        }

        protected String getStringField(String column, boolean required) throws IOException {
            // TODO deduplicate strings
            String str = reader.get(column);
            if (required && (str == null || str.isEmpty())) {
                errorList.add(new EmptyFieldError(tableName, row, column));
            }
            return str;
        }

        protected int getIntField(String column, boolean required) throws IOException {
            String str = null;
            int val = INT_MISSING;
            try {
                str = reader.get(column);
                if (str == null || str.isEmpty()) {
                    if (required) {
                        errorList.add(new EmptyFieldError(tableName, row, column));
                    } else {
                        val = 0; // TODO && emptyMeansZero
                    }
                } else {
                    val = Integer.parseInt(str);
                }
            } catch (NumberFormatException nfe) {
                errorList.add(new NumberParseError(tableName, row, column));
            }
            return val;
        }

        protected int getTimeField(String column) throws IOException {
            String str = null;
            int val = -1;
            try {
                str = reader.get(column);
                String[] fields = str.split(":");
                if (fields.length != 3) {
                    errorList.add(new TimeParseError(tableName, row, column));
                } else {
                    int hours = Integer.parseInt(fields[0]);
                    int minutes = Integer.parseInt(fields[1]);
                    int seconds = Integer.parseInt(fields[2]);
                    val = (hours * 60 * 60) + minutes * 60 + seconds;
                }
            } catch (NumberFormatException nfe) {
                errorList.add(new TimeParseError(tableName, row, column));
            }
            return val;
        }

        // TODO add range checking parameters, with private function that can record out-of-range errors
        protected double getDoubleField(String column, boolean required) throws IOException {
            String str = null;
            double val = 0;
            try {
                str = reader.get(column);
                if (required && (str == null || str.isEmpty())) {
                    errorList.add(new EmptyFieldError(tableName, row, column));
                    val = -1;
                } else {
                    val = Double.parseDouble(str);
                }
            } catch (NumberFormatException nfe) {
                errorList.add(new NumberParseError(tableName, row, column));
            }
            return val;
        }

        protected boolean checkRequiredColumns() throws IOException {
            boolean missing = false;
            for (String column : requiredColumns) {
                if (reader.getIndex(column) == -1) {
                    errorList.add(new MissingColumnError(tableName, column));
                    missing = true;
                }
            }
            return missing;
        }

        protected abstract E fromCsv() throws IOException;

        // New parameter K inferred from map. Parameter E is the entity type from the containing class.
        public <K> void loadTable(ZipFile zip, List<GTFSError> errorList, Map<K, E> targetMap) throws IOException {
            this.errorList = errorList;
            ZipEntry entry = zip.getEntry(tableName + ".txt");
            if (entry == null) {
                /* This GTFS table did not exist in the zip. */
                if (required) {
                    String msg = String.format("Required table '%s' was not present.", tableName);
                    errorList.add(new MissingTableError(tableName));
                }
                return;
            }
            LOG.info("Loading GTFS table {} from {}", tableName, entry);
            InputStream zis = zip.getInputStream(entry);
            CsvReader reader = new CsvReader(zis, ',', Charset.forName("UTF8"));
            this.reader = reader;
            reader.readHeaders();
            checkRequiredColumns();
            while (reader.readRecord()) {
                // reader.getCurrentRecord() is zero-based and does not include the header line, keep our own row count
                if (++row % 500000 == 0) {
                    LOG.info("Record number {}", human(row));
                }
                E entity = fromCsv(); // Call subclass method to produce an entity from the current row.
                targetMap.put((K)(entity.getKey()), entity);
            }
        }

        private static String human (long n) {
            if (n >= 1000000) return String.format("%.1fM", n/1000000.0);
            if (n >= 1000) return String.format("%.1fk", n/1000.0);
            else return String.format("%d", n);
        }

    }

}

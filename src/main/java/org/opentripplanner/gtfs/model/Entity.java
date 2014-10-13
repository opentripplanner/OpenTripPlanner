package org.opentripplanner.gtfs.model;

import com.beust.jcommander.internal.Lists;
import com.csvreader.CsvReader;
import org.opentripplanner.routing.trippattern.Deduplicator;
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
 * A class that represents a row in a GTFS table, e.g. a Stop, Trip, or Agency.
 * Created by abyrd on 2014-10-12
 */
// TODO K is the key type for this table
public abstract class Entity {

    /* The ID of the feed from which this entity was loaded. */
    String feedId;

    public abstract Object getKey();


    /* A class that can produce Entities from CSV, and record errors that occur in the process. */
    // This is almost a GTFSTable... rename?
    public static abstract class Factory<E extends Entity> {

        private static final Logger LOG = LoggerFactory.getLogger(Factory.class);

        // TODO private static final StringDeduplicator;

        String tableName; // name of corresponding table without .txt
        String[] requiredColumns;

        CsvReader reader;
        List<Error> errorList = Lists.newArrayList();

        public String getStrField(String column) throws IOException {
            // TODO deduplicate strings
            String str = reader.get(column);
            return str;
        }

        public int getIntField(String column) throws IOException {
            String str = null;
            int val = -1;
            try {
                str = reader.get(column);
                val = Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                String msg = String.format("Error parsing int from string '%s' from column '%s' at line number X. " +
                        "This does not look like an integer.", str, column);
                errorList.add(new Error(msg));
            }
            return val;
        }

        public double getDblField(String column) throws IOException {
            String str = null;
            double val = 0;
            try {
                str = reader.get(column);
                val = Double.parseDouble(str);
            } catch (NumberFormatException nfe) {
                String msg = String.format("Error parsing double from string '%s' from column '%s' at line number X. " +
                        "This does not look like an double.", str, column);
                errorList.add(new Error(msg));
            }
            return val;
        }

        private boolean checkRequiredColumns() throws IOException {
            boolean missing = false;
            for (String column : requiredColumns) {
                if (reader.getIndex(column) == -1) {
                    errorList.add(new Error(String.format("Missing required column '%s' in table X.", column)));
                    missing = true;
                }
            }
            return missing;
        }

        public abstract E fromCsv() throws IOException;

        // New parameter K inferred from map. Parameter E is the entity type from the containing class.
        public <K> void loadTable(ZipFile zip, List<Error> errorList, Map<K, E> targetMap) throws IOException {
            this.errorList = errorList;
            ZipEntry entry = zip.getEntry(tableName + ".txt");
            LOG.info("Loading GTFS table {} from {}", tableName, entry);
            InputStream zis = zip.getInputStream(entry);
            CsvReader reader = new CsvReader(zis, ',', Charset.forName("UTF8"));
            this.reader = reader;
            reader.readHeaders();
            checkRequiredColumns();
            int rec = 0;
            while (reader.readRecord()) {
                if (++rec % 500000 == 0) {
                    LOG.info("Record number {}", human(rec));
                }
                E entity = fromCsv(); // Call subclass method to produce an entity from the current row.
                targetMap.put((K)(entity.getKey()), entity);
            }
        }

        private static String human (int n) {
            if (n > 1000000) return String.format("%.1fM", n/1000000.0);
            if (n > 1000) return String.format("%.1fk", n/1000.0);
            else return String.format("%d", n);
        }

    }

}

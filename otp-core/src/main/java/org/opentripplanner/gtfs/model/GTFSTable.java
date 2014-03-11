package org.opentripplanner.gtfs.model;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvReader;

// generic-parameterizing this is kind of useless because we need the type at runtime
// to examine the fields.
public class GTFSTable {
    
    private static final Logger LOG = LoggerFactory.getLogger(GTFSTable.class);
    final String name; // without .txt
    final Class<? extends GtfsEntity> entityClass;
    final boolean optional;
    final GtfsField[] fields;
    //Map<K, E> entities;

    public static GtfsField[] getGtfsFields(Class<? extends GtfsEntity> entityClass) {
        Field[] fields = entityClass.getFields();
        GtfsField[] gfields = new GtfsField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            boolean required = fields[i].isAnnotationPresent(Required.class);
            gfields[i] = new GtfsField(name, !required);
        }
        return gfields;
    }

    public GTFSTable(String name, Class<? extends GtfsEntity> entityClass, boolean optional) {
        this.name = name;
        this.entityClass = entityClass;
        this.optional = optional;
        this.fields = getGtfsFields(entityClass);
        // should entities be in here, or returned? should be returned, using target field's generic types.
    }

    // Method infers the types from the target Map.
    @SuppressWarnings("unchecked")
    public <K, V> void loadTable(ZipFile zip, Map<K, V> target) throws Exception {
        ZipEntry entry = zip.getEntry(name + ".txt");
        LOG.info("Loading GTFS table {} from {}", name, entry);
        InputStream zis = zip.getInputStream(entry);                
        CsvReader reader = new CsvReader(zis, ',', Charset.forName("UTF8"));
        reader.readHeaders();
        for (GtfsField field : fields) {
            field.col = reader.getIndex(field.name);
            if (field.col < 0 && !field.optional) {
                LOG.error("Missing required column {} in table '{}'.", field.name, name);
            }
        }
        int rec = 0;
        while (reader.readRecord()) {
            if (++rec % 500000 == 0) {
                LOG.info("Record number {}", human(rec));
            }
            String[] row = new String[fields.length];
            int col = 0;
            for (GtfsField field : fields) {
                if (field.col >= 0) {
                    String val = reader.get(field.col);
                    row[col] = val; //dedup.dedup(val);
                }
                col++;
            }
            GtfsEntity e = entityClass.newInstance();
            e.setFromStrings(row);
            target.put((K)e.getKey(), (V)e);
        }
    }
    
    private static String human (int n) {
        if (n > 1000000) return String.format("%.1fM", n/1000000.0); 
        if (n > 1000) return String.format("%.1fk", n/1000.0); 
        else return String.format("%d", n);
    }
    
}

// Add string-parsing functions as in QueryScraper
class GtfsField {
    final String name;
    final boolean optional;
    int col;
    public GtfsField(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
    }
}


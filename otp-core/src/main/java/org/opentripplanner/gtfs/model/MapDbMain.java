package org.opentripplanner.gtfs.model;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import com.beust.jcommander.internal.Lists;
import com.csvreader.CsvReader;

public class MapDbMain {

    static final List<Class<? extends GtfsEntity>> gtfsTables;
    static {
        gtfsTables = Lists.newArrayList();
        gtfsTables.add(Agency.class);
        gtfsTables.add(Route.class);
        gtfsTables.add(Stop.class);
        gtfsTables.add(Trip.class);
        gtfsTables.add(StopTime.class);
    }
        
    static final String INPUT = "/var/otp/graphs/nl/gtfs-nl.zip";        
//    /static final String INPUT = "/var/otp/graphs/trimet/gtfs.zip";        
    static final String DB = "/home/abyrd/tmp/gtfs-mapdb";
    
    private static String human (int n) {
        if (n > 1000000) return String.format("%.1fM", n/1000000.0); 
        if (n > 1000) return String.format("%.1fk", n/1000.0); 
        else return String.format("%d", n);
    }
    
    @SuppressWarnings("rawtypes")
    public static void main (String[] args) {
        DB db = DBMaker.newFileDB(new File(DB))
            .transactionDisable().asyncWriteEnable().make();
        List<Error> errors = Lists.newArrayList();
        try {
            ZipFile zipfile = new ZipFile(INPUT);
            for (Class<? extends GtfsEntity> entityClass : gtfsTables) {
                GtfsEntity entity = entityClass.newInstance();
                GtfsTable table = new GtfsTable(
                        entity.getFilename(), false, 
                        entity.getGtfsFields(), entityClass);
                System.out.printf("Reading GTFS table '%s' (%soptional)\n", 
                    table.name, table.optional ? "" : "not ");
                // check if DB already exists
                table.entities = db.getTreeMap(table.name);
                ZipEntry entry = zipfile.getEntry(table.name);
                InputStream zis = zipfile.getInputStream(entry);                
                CsvReader reader = new CsvReader(zis, ',', Charset.forName("UTF8"));
                reader.readHeaders();
                for (GtfsField field : table.fields) {
                    field.col = reader.getIndex(field.name);
                    if (field.col < 0 && !field.optional) {
                        String message =  String.format(
                            "Missing required field %s in file %s.",
                            field.name, table.name);
                        errors.add(new Error(message));
                    }
                }
                int rec = 0;
                while (reader.readRecord()) {
                    if (++rec % 100000 == 0) {
                        System.out.println(human(rec));
                    }
                    String[] row = new String[table.fields.length];
                    int col = 0;
                    for (GtfsField field : table.fields) {
                        if (field.col >= 0) {
                            String val = reader.get(field.col);
                            row[col] = val; //dedup.dedup(val);
                        }
                        col++;
                    }
                    GtfsEntity e = table.klass.newInstance();
                    e.setFromStrings(row);
                    table.entities.put(e.getKey(), e);
                }
            }
            for (Error e : errors) System.out.println(e.message);
            zipfile.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // As a test, find all stop patterns
        Map<String, Trip> trips = db.getTreeMap("trips.txt");
        ConcurrentNavigableMap<Fun.Tuple2, StopTime> stopTimes = 
            db.getTreeMap("stop_times.txt");
        Set<List<String>> patterns = db.getHashSet("patterns");
        int t = 0;
        for (String trip_id : trips.keySet()) {
            if (++t % 10000 == 0) {
                System.out.printf("trip %s\n", human(t));
            }
            Map<Fun.Tuple2, StopTime> tripStopTimes = 
                stopTimes.subMap(
                    Fun.t2(trip_id, null),
                    Fun.t2(trip_id, Fun.HI)
                );
            List<String> stops = Lists.newArrayList();
            // in-order traversal
            for (StopTime stopTime : tripStopTimes.values()) {
                stops.add(stopTime.stop_id);
            }
            patterns.add(stops);
        }
        System.out.printf("Total patterns: %d\n", patterns.size());
        //db.compact();
        db.close();
    }
}

@RequiredArgsConstructor
class GtfsTable {
    final String name; // without .txt
    final boolean optional;
    final GtfsField[] fields;
    final Class<? extends GtfsEntity> klass;
    Map<Object, GtfsEntity> entities;
}

@RequiredArgsConstructor
class GtfsField {
    final String name;
    final boolean optional;
    int col;
}

@AllArgsConstructor
class Error {
    String message;
}

class StringDeduplicator {
    Map <String, String> map;
    public StringDeduplicator (DB db) {
        this.map = db.getTreeMap("strings");
        //this.map = Maps.newHashMap();
    }
    public String dedup (String s) {
        if (s == null) return null;
        String existing = map.get(s);
        if (existing != null) return existing;
        map.put(s, s);
        return s;
    }
}


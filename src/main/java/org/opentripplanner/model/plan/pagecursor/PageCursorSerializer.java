package org.opentripplanner.model.plan.pagecursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PageCursorSerializer {
    private static final int NOT_SET = Integer.MIN_VALUE;
    private static final long TIME_0 = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toEpochSecond();
    private static final Logger LOG = LoggerFactory.getLogger(PageCursor.class);

    /** private constructor to prevent instantiating this utility class */
    private PageCursorSerializer() { /* empty */ }

    @Nullable
    public static String encode(PageCursor cursor) {
        var buf = new ByteArrayOutputStream();
        try(var out = new ObjectOutputStream(buf)){
            // The order must be the same in the encode and decode function
            writeTime(cursor.earliestDepartureTime, out);
            writeTime(cursor.latestArrivalTime, out);
            writeDuration(cursor.searchWindow, out);
            writeBoolean(cursor.reverseFilteringDirection, out);
            out.flush();
            return Base64.getUrlEncoder().encodeToString(buf.toByteArray());
        }
        catch (IOException e) {
            LOG.error("Failed to encode page cursor", e);
            return null;
        }
    }

    @Nullable
    public static PageCursor decode(String cursor) {
        if(cursor == null) { return null; }

        var buf = Base64.getUrlDecoder().decode(cursor);
        var input = new ByteArrayInputStream(buf);

        try(var in = new ObjectInputStream(input)) {
            // The order must be the same in the encode and decode function
            var edt = readTime(in);
            var lat = readTime(in);
            var searchWindow = readDuration(in);
            var reverseFilteringDirection = readBoolean(in);
            return new PageCursor(edt, lat, searchWindow, reverseFilteringDirection);
        }
        catch (IOException e) {
            LOG.error("Unable to decode page cursor: '" + cursor + "'", e);
            return null;
        }
    }

    private static void writeTime(Instant time, ObjectOutputStream out) throws IOException {
        out.writeInt(time == null ? NOT_SET : (int)(time.getEpochSecond() - TIME_0));
    }

    @Nullable
    private static Instant readTime(ObjectInputStream in) throws IOException {
        var value = in.readInt();
        return value == NOT_SET ? null : Instant.ofEpochSecond(TIME_0 + value);
    }

    private static void writeDuration(Duration duration, ObjectOutputStream out) throws IOException {
        out.writeInt((int)duration.toSeconds());
    }

    private static Duration readDuration(ObjectInputStream in) throws IOException {
        return Duration.ofSeconds(in.readInt());
    }

    private static void writeBoolean(boolean value, ObjectOutputStream out) throws IOException {
        out.writeBoolean(value);
    }

    private static boolean readBoolean(ObjectInputStream in) throws IOException {
        return in.readBoolean();
    }
}

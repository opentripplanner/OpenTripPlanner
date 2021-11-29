package org.opentripplanner.model.plan;

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
import org.opentripplanner.model.base.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class hold all the information needed to page to the next/previous page. It is
 * serialized as base64 when passed on to the client. The base64 encoding is done to prevent the
 * client from using the information inside the cursor.
 * <p>
 * This class is internal to the router, only the serialized string is passed to/from the clients.
 */
public class PageCursor {
    private static final int NOT_SET = Integer.MIN_VALUE;
    private static final long TIME_0 = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toEpochSecond();
    private static final Logger LOG = LoggerFactory.getLogger(PageCursor.class);

    public final Instant earliestDepartureTime;
    public final Instant latestArrivalTime;
    public final Duration searchWindow;

    private PageCursor(
            Instant earliestDepartureTime,
            Instant latestArrivalTime,
            Duration searchWindow
    ) {
        this.earliestDepartureTime = earliestDepartureTime;
        this.latestArrivalTime = latestArrivalTime;
        this.searchWindow = searchWindow;
    }

    public static PageCursor arriveByCursor(
            Instant earliestDepartureTime, Instant latestArrivalTime, Duration searchWindow
    ) {
        return new PageCursor(earliestDepartureTime, latestArrivalTime, searchWindow);
    }

    public static PageCursor departAfterCursor(
            Instant earliestDepartureTime, Duration searchWindow
    ) {
        return new PageCursor(earliestDepartureTime, null, searchWindow);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(PageCursor.class)
                .addTime("edt", earliestDepartureTime)
                .addTime("lat", latestArrivalTime)
                .addDuration("searchWindow", searchWindow)
                .toString();
    }

    @Nullable
    public String encode() {
        var buf = new ByteArrayOutputStream();
        try(var out = new ObjectOutputStream(buf)){
            // The order must be the same in the encode and decode function
            writeTime(earliestDepartureTime, out);
            writeTime(latestArrivalTime, out);
            writeDuration(searchWindow, out);
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
            return new PageCursor(edt, lat, searchWindow);
        }
        catch (IOException e) {
            LOG.error("Unable to decode page cursor: '" + cursor + "'", e);
            return null;
        }
    }

    public Instant nextDateTime() {
        return arriveBy() ? latestArrivalTime : earliestDepartureTime;
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

    private boolean arriveBy() {
        return latestArrivalTime != null;
    }
}

package org.opentripplanner.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by abyrd on 2019-10-15
 */
public class ThrowableUtils {

    public static String detailedString (Throwable throwable) {
        StringWriter sw = new StringWriter();
        sw.append(throwable.getMessage());
        sw.append("\n");
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}

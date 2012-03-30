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
package org.opentripplanner.util;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntUtils {
    private static final Logger LOG = LoggerFactory.getLogger(IntUtils.class);
    public  static final String POINT_PREFIX = "POINT("; 
    
    /** does this string appear to be a coordinate of some sort */
    public static boolean looksLikeCoordinate(String str)
    {
        if(str != null && (str.contains(POINT_PREFIX) || str.matches("[\\s]*[0-9\\-.]+[,\\s]+[0-9\\-.]+[\\s]*")))
            return true;

        return false;
    }

    public static final double roundDouble(double d, int places) {
        return Math.round(d * Math.pow(10, places)) / Math.pow(10, places);
    }

    /** take a string of ints (eg: 1,2,3,4), and return a List of Integers */
    static public List<Integer> asList(String str) {
        return asList(str, ",");
    }

    /** take a string of ints (eg: 1[sep]2[sep]3[sep]4), and return a List of Integers */
    static public List<Integer> asList(String str, String sep) {
        List<Integer> retVal = new ArrayList<Integer>();
        try {
            String[] s = str.split(sep);
            for (int i = 0; i < s.length; i++) {
                Integer k = null;
                try {
                    k = Integer.parseInt(s[i].trim());
                } catch (Exception m) {
                }
                if (k != null)
                    retVal.add(k);
            }
        } catch (Exception e) {
        }

        return retVal;
    }

    static public Integer getZipFromString(String zipStr) {
        Integer retVal = null;
        try {
            retVal = getIntegerFromSubString(zipStr, "-");
            if (retVal == null) {
                retVal = getIntegerFromString(zipStr);
            }
        } catch (Exception e) {
            retVal = null;
        }

        return retVal;
    }

    public static short getShortFromString(String input) {
        return (short) getIntFromString(input);
    }

    public static int getIntFromString(String input) {
        int retVal = 0;

        Integer n = getIntegerFromString(input);
        if (n != null)
            retVal = n.intValue();
        else if (input != null)
            retVal = input.hashCode();

        return retVal;
    }

    public static Integer getIntegerFromString(String input) {
        try {
            return new Integer(input);
        } catch (Exception e) {
            try {
                String in = input.replaceAll("\\D", "");
                return new Integer(in);
            } catch (Exception ee) {
                return null;
            }
        }
    }

    public static Integer getIntegerFromString(String input, Integer def) {
        Integer retVal = getIntegerFromString(input);
        if (retVal == null)
            retVal = def;

        return retVal;
    }

    public static Integer getIntegerFromSubString(String input, int len) {
        String tmp = input.substring(len);
        return getIntegerFromString(tmp.trim());
    }

    /**
     * expect an Integer between prefix and suffix
     * 
     * eg: if is this is our string "Hi there #2112, how are you" then a call of
     * getIntegerFromSubString("Hi there #2112, how are you", "#", ","); will return 2112
     * 
     * note: if " " is specified, and there is no space from prefix to end of line, then the whole
     * line is evaluated
     * 
     * @param target
     * @param prefix
     * @param suffix
     * @return
     */
    public static Integer getIntegerFromSubString(String target, String prefix, String suffix) {
        if (target == null)
            return null;

        Integer retVal = null;
        try {
            String tmp = target;
            if (prefix != null && target.contains(prefix)) {
                // get the line from the end of the prefix to end of line
                int sz = prefix.length();
                int in = target.indexOf(prefix);
                tmp = target.substring(in + sz);
            }

            if (tmp != null && suffix != null && target.contains(suffix)) {
                // get suffix endpoint -- and compensate for whitespace / end of line (same thing)
                int suf = tmp.indexOf(suffix);
                if (suf <= 0 && suffix.equals(" "))
                    suf = tmp.length();

                tmp = tmp.substring(0, suf);
                retVal = IntUtils.getIntegerFromString(tmp.trim());
            }
        } catch (Exception e) {
            // not too big a deal if this dies...just return null, as if we couldn't find an int
            // there
            LOG.info("Not a big deal that we couldn't find an int from substring...going to return null", e);
            retVal = null;
        }

        return retVal;
    }

    public static Integer getIntegerFromSubString(String target, String suffix) {
        return getIntegerFromSubString(target, null, suffix);
    }

    public static Double getDoubleFromString(String input) {
        try {
            return new Double(input);
        } catch (Exception e) {
            LOG.info("Not a big deal...going to return null", e);
            return null;
        }
    }

    public static long getLongFromString(String input) {
        return getLongFromString(input, -111);
    }

    public static long getLongFromString(String input, long def) {
        try {
            return new Long(input);
        } catch (Exception e) {
            LOG.info("Not a big deal...going to return default value", e);
            return def;
        }
    }
}

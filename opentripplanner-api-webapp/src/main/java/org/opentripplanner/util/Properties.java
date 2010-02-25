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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The purpose of Properties is to easily read a ResourceBundel (set of localized .properties files), and get the named contents.
 * Goes really well with an enumerated type (@see org.opentripplanner.api.ws.Message)
 */
@SuppressWarnings("unchecked")
public class Properties {

    public static final Logger LOGGER = Logger.getLogger(Properties.class.getCanonicalName());

    private final String _bundle; 

    public Properties() {
        this(Properties.class);
    }

    public Properties(Class c) {
        _bundle = c.getSimpleName();
    }

    public Properties(String bun) {
        _bundle = bun;
    }

    /** 
     * static .properties resource loader
     * will first look for a resource org.opentripplaner.blah.blah.blah.ClassName.properties.
     * if that doesn't work, it searches for ClassName.properties.
     */
    public static ResourceBundle getBundle(String name, Locale l) {
        try {
            return ResourceBundle.getBundle(name, l);
        }
        catch(Exception e) {
            Properties.LOGGER.log(Level.ALL, "Uh oh...no .properties file could be found, so things are most definately not going to turn out well!!!", e);
        }
        return null;
    }

    public synchronized String get(String name, Locale l) throws Exception {
        ResourceBundle rb = getBundle(_bundle, l);
        return rb.getString(name);
    }
    public synchronized String get(String name) throws Exception {
        ResourceBundle rb = getBundle(_bundle, Locale.getDefault());
        return rb.getString(name);
    }


    public String get(String name, String def, Locale l) {
        String retVal = null;
        try {
            retVal = get(name, l);
        } catch (Exception _) {
        }

        if (retVal == null || retVal.length() < 1)
            retVal = def;

        return retVal;
    }
    public String get(String name, String def) {
        return get(name, def, Locale.getDefault());
    }


    public boolean get(String name, boolean def, Locale l) {
        boolean retVal = def;
        try {
            String s = get(name, l);
            if (s.toLowerCase().equals("true"))
                retVal = true;
        } catch (Exception e) {
            retVal = def;
        }

        return retVal;
    }
    public boolean get(String name, boolean def) {
        return get(name, def, Locale.getDefault());
    }

    
    public int get(String name, int def, Locale l) {
        String tmp = get(name, Integer.toString(def), l);
        return IntUtils.getIntFromString(tmp);
    }
    public int get(String name, int def) {
        return get(name, def, Locale.getDefault());
    }

    public long get(String name, long def, Locale l) {
        String tmp = get(name, Long.toString(def), l);
        return IntUtils.getLongFromString(tmp);
    }
    public long get(String name, long def) {
        return get(name, def, Locale.getDefault());
    }


    public boolean is(String name, Locale l) {
        boolean retVal = false;
        try {
            String r = get(name, l);
            if (r != null && r.equalsIgnoreCase("true"))
                retVal = true;
        } catch (Exception _) {
        }

        return retVal;
    }
    public boolean is(String name) {
        return is(name, Locale.getDefault());
    }


    public synchronized String format(String name, Locale l, Object... args) {
        try {
            ResourceBundle rb = getBundle(_bundle, l);
            return MessageFormat.format(rb.getString(name), args);
        } catch (Exception e) {
            LOGGER.log(Level.CONFIG, "couldn't find / format property " + name + "; returning null", e);
        }

        return null;
    }
    public synchronized String format(String name, Object... args) {
        return format(name, Locale.getDefault(), args);
    }
}

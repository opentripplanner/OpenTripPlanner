package org.opentripplanner.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of Properties is to easily read a ResourceBundel (set of localized .properties files), and get the named contents.
 * Goes really well with an enumerated type (@see org.opentripplanner.api.ws.Message)
 */
public class Properties {

    public static final Logger LOG = LoggerFactory.getLogger(Properties.class);

    private final String bundle;

    public Properties() {
        this(Properties.class);
    }

    public Properties(Class<?> c) {
        bundle = c.getSimpleName();
    }

    public Properties(String bundle) {
        this.bundle = bundle;
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
            LOG.error("Uh oh...no .properties file could be found, so things are most definately not going to turn out well!!!", e);
        }
        return null;
    }

    public synchronized String get(String name, Locale l) throws Exception {
        ResourceBundle rb = getBundle(bundle, l);
        return rb.getString(name);
    }
    public synchronized String get(String name) throws Exception {
        ResourceBundle rb = getBundle(bundle, Locale.getDefault());
        return rb.getString(name);
    }


    public String get(String name, String def, Locale l) {
        String retVal = null;
        try {
            retVal = get(name, l);
        }
        catch (Exception ex) { }

        if (retVal == null || retVal.length() < 1) {
            retVal = def;
        }

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
        } catch (Exception ex) {
        }

        return retVal;
    }
    public boolean is(String name) {
        return is(name, Locale.getDefault());
    }


    public synchronized String format(String name, Locale l, Object... args) {
        try {
            ResourceBundle rb = getBundle(bundle, l);
            return MessageFormat.format(rb.getString(name), args);
        } catch (Exception e) {
            LOG.warn("couldn't find / format property " + name + "; returning null", e);
        }

        return null;
    }
    public synchronized String format(String name, Object... args) {
        return format(name, Locale.getDefault(), args);
    }
}

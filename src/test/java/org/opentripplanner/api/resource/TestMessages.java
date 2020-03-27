package org.opentripplanner.api.resource;

import junit.framework.TestCase;
import org.opentripplanner.api.common.Message;

import java.util.Locale;

public class TestMessages extends TestCase {

    public void testLanguages() {
        // Keep default locale so we can set it back after the test is done
        Locale sysDefaultLocale = Locale.getDefault();
        try {
            // Force default to make test work on non-US machines
            Locale.setDefault(new Locale("en", "US"));

            String e = Message.PATH_NOT_FOUND.get();
            String f = Message.PATH_NOT_FOUND.get(Locale.CANADA_FRENCH);
            String s = Message.PATH_NOT_FOUND.get(new Locale("es"));

            TestCase.assertNotNull(e);
            TestCase.assertNotNull(f);
            TestCase.assertNotNull(s);
            TestCase.assertNotSame(e, f);
            TestCase.assertNotSame(e, s);
            TestCase.assertNotSame(f, s);
        }
        finally {
            Locale.setDefault(sysDefaultLocale);
        }
    }
}

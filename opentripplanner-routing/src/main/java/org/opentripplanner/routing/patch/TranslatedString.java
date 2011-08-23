package org.opentripplanner.routing.patch;

import java.io.Serializable;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlType
public class TranslatedString implements Serializable {
    private static final long serialVersionUID = 2163930399727941628L;

    @XmlElement
    @XmlJavaTypeAdapter(MapAdaptor.class)
    public TreeMap<String, String> translations = new TreeMap<String, String>();

    public TranslatedString(String language, String note) {
        translations.put(language.intern(), note);
    }

    public TranslatedString() {
    }

    public boolean equals(Object o) {
        if (!(o instanceof TranslatedString)) {
            return false;
        }
        TranslatedString tso = (TranslatedString) o;
        return tso.translations.equals(translations);
    }

    public int hashCode() {
        return translations.hashCode() + 1;
    }

    public void addTranslation(String language, String note) {
        translations.put(language.intern(), note);
    }

    // fixme: need to get en-US when requested language is "en"
    public String getTranslation(String language) {
        return translations.get(language);
    }

    public String getSomeTranslation() {
        return translations.values().iterator().next();
    }
}

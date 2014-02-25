package org.opentripplanner.gtfs.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import com.beust.jcommander.internal.Lists;

@interface Required {}

@interface Key {}

abstract class GtfsEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * This is not a static method because it must be overridden in one subclass.
     */
    public List<Error> checkRequiredFields(Collection<String> present) {
        List<Error> errors = Lists.newArrayList();
        for (Field field : getClass().getFields()) {
            if (field.isAnnotationPresent(Required.class)) {
                if (!present.contains(field.getName())) {
                    errors.add(new Error("Missing required field " + field.getName()));
                }
            }
        }
        return errors;
    }

    public List<Error> setFromStrings(String[] strings) {
        List<Error> errors = Lists.newArrayList();
        int i = 0;
        for (Field field : getClass().getFields()) {
            try {
                if (strings[i] != null) {
                    field.set(this, strings[i]);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                errors.add(new Error("Missing fields in row."));
                break;
            } catch (Exception e) {
                errors.add(new Error("Exception: " + e));
            }
            i++;
        }
        return errors;
    }

    public GtfsField[] getGtfsFields() {
        Field[] fields = getClass().getFields();
        GtfsField[] gfields = new GtfsField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            boolean required = fields[i].isAnnotationPresent(Required.class);
            gfields[i] = new GtfsField(name, !required);
        }
        return gfields;
    }

    public abstract Object getKey();

    public String getFilename() {
        return this.getClass().getSimpleName().toLowerCase() + "s.txt";
    }

    public boolean fileIsRequired() {
        return true;
    }
    
}
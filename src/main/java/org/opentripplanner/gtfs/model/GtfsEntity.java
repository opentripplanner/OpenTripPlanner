package org.opentripplanner.gtfs.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;

/** Annotation for fields that are required in the GTFS spec. */
@interface Required {}

abstract class GtfsEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(GtfsEntity.class);

    /**
     * This is not a static method because it must be overridden in one subclass.
     * 
     * Are we even using this?
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

    public void setFromStrings(String[] strings) {
        int i = 0;
        for (Field field : getClass().getFields()) {
            try {
                if (strings[i] != null) {
                    // TODO adapt for numeric fields
                    field.set(this, strings[i]);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                LOG.error("Missing fields in row: ");
                break;
            } catch (Exception e) {
                LOG.error("Exception: " + e);
                throw new RuntimeException(e);
            }
            i++;
        }
    }

    public abstract Object getKey();
    
}
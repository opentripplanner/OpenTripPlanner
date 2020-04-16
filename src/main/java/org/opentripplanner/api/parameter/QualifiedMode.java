package org.opentripplanner.api.parameter;

import com.google.common.collect.Sets;
import javax.ws.rs.BadRequestException;
import java.io.Serializable;
import java.util.Set;

public class QualifiedMode implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public final ApiRequestMode mode;
    public final Set<Qualifier> qualifiers = Sets.newHashSet();

    public QualifiedMode(String qMode) {
        try {
            String[] elements = qMode.split("_");
            mode = ApiRequestMode.valueOf(elements[0].trim());
            for (int i = 1; i < elements.length; i++) {
                Qualifier q = Qualifier.valueOf(elements[i].trim());
                qualifiers.add(q);
            }
        }
        catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new BadRequestException(
                    "Qualified mode is not valid: '" + qMode + "', details: " + e.getMessage()
            );
        }
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mode);
        for (Qualifier qualifier : qualifiers) {
            sb.append("_");
            sb.append(qualifier);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return mode.hashCode() * qualifiers.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof QualifiedMode) {
            QualifiedMode qmOther = (QualifiedMode) other;
            return qmOther.mode.equals(this.mode) && qmOther.qualifiers.equals(this.qualifiers);
        }
        return false;
    }

}
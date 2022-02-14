package org.opentripplanner.netex.mapping;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.KeyValue;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;

/** Responsible for mapping NeTEx key value structure into OTP model */
public class KeyValueMapper {

    public List<KeyValue> mapKeyValues(KeyListStructure keyListStructure) {
        if (keyListStructure == null || keyListStructure.getKeyValue() == null) {
            return null;
        }

        return keyListStructure.getKeyValue().stream().map(this::mapKeyValue).collect(
                Collectors.toList());
    }

    private KeyValue mapKeyValue(KeyValueStructure netexKeyValue) {
        return new KeyValue(netexKeyValue.getTypeOfKey(), netexKeyValue.getKey(), netexKeyValue.getValue());
    }
}

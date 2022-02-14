package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.Test;
import org.opentripplanner.model.KeyValue;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;

public class KeyValueMapperTest {

    private final String TYPE_OF_KEY = "TEST_TYPE";
    private final String KEY = "TEST_KEY";
    private final String VALUE = "TEST_VALUE";

    @Test
    public void mapKeyValues() {
       KeyValueMapper keyValueMapper = new KeyValueMapper();

       KeyListStructure keyListStructure = createKeyListStructure();
       List<KeyValue> result = keyValueMapper.mapKeyValues(keyListStructure);

       assertEquals(1, result.size());
       KeyValue keyValue = result.get(0);
       assertEquals(TYPE_OF_KEY, keyValue.getTypeOfKey());
       assertEquals(KEY, keyValue.getKey());
       assertEquals(VALUE, keyValue.getValue());
    }

    public KeyListStructure createKeyListStructure() {
        return new KeyListStructure()
                .withKeyValue(new KeyValueStructure()
                        .withTypeOfKey(TYPE_OF_KEY)
                        .withKey(KEY)
                        .withValue(VALUE));
    }
}

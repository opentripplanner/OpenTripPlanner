package org.opentripplanner.hasura_client.mappers;


import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public abstract class HasuraToOTPMapper<HASURA_OBJECT, OTP_OBJECT> {
    protected abstract OTP_OBJECT mapSingleHasuraObject(HASURA_OBJECT hasuraObject);

    public List<OTP_OBJECT> map(List<HASURA_OBJECT> objects) {
        return objects.stream()
                .map(this::mapSingleHasuraObject)
                .filter(Objects::nonNull)
                .collect(toList());
    }
}

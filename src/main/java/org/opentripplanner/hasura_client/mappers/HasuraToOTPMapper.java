package org.opentripplanner.hasura_client.mappers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public abstract class HasuraToOTPMapper<H, G> {
    protected abstract G mapSingleHasuraObject(H hasuraObject);

    protected static final Logger LOG = LoggerFactory.getLogger(HasuraToOTPMapper.class);

    public List<G> map(List<H> objects) {
        return objects.stream()
                .map(this::mapSingleHasuraObject)
                .filter(Objects::nonNull)
                .collect(toList());
    }
}

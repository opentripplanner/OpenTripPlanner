package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.ResourceFrame;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;

class ResourceFrameParser {

    private final Collection<Authority> authorityById = new ArrayList<>();

    void parse(ResourceFrame resourceFrame) {
        for (JAXBElement e : resourceFrame.getOrganisations().getOrganisation_()) {
            if(e.getValue() instanceof Authority){
                Authority authority = (Authority) e.getValue();
                authorityById.add(authority);
            }
        }
    }

    void setResultOnIndex(NetexImportDataIndex netexIndex) {
        netexIndex.authoritiesById.addAll(authorityById);
    }
}

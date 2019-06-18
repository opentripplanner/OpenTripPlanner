package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.ResourceFrame;

import javax.xml.bind.JAXBElement;
import java.util.Collection;

class ResourceFrameParser {

    private final HierarchicalMapById<Authority> authorityById = new HierarchicalMapById<>();

    void parse(ResourceFrame resourceFrame) {
        Collection<JAXBElement<? extends DataManagedObjectStructure>> organisations = resourceFrame
                .getOrganisations().getOrganisation_();
        for (JAXBElement element : organisations) {
            if(element.getValue() instanceof Authority){
                Authority authority = (Authority) element.getValue();
                authorityById.add(authority);
            }
        }
    }

    HierarchicalMapById<Authority> getAuthorityById() {
        return authorityById;
    }
}

package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.ResourceFrame;

import javax.xml.bind.JAXBElement;
import java.util.List;

public class ResourceFrameParser {

    private final HierarchicalMapById<Authority> authorityById = new HierarchicalMapById<>();

    public void parse(ResourceFrame resourceFrame) {
        List<JAXBElement<? extends DataManagedObjectStructure>> organisations = resourceFrame
                .getOrganisations().getOrganisation_();
        for (JAXBElement element : organisations) {
            if(element.getValue() instanceof Authority){
                Authority authority = (Authority) element.getValue();
                authorityById.add(authority);
            }
        }
    }

    public HierarchicalMapById<Authority> getAuthorityById() {
        return authorityById;
    }
}

package org.opentripplanner.netex;

import org.junit.Test;
import org.rutebanken.netex.model.PublicationDeliveryStructure;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static org.junit.Assert.assertTrue;

public class UnmarshalTest {

    @Test
    public void testUnmarshal() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
        assertTrue(jaxbContext instanceof JAXBContext);
    }
}
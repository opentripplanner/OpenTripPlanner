package org.opentripplanner.ext.trias.trias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.io.FileUtils.assertFileEquals;
import static org.opentripplanner.framework.io.FileUtils.readFile;
import static org.opentripplanner.framework.io.FileUtils.writeFile;

import de.vdv.ojp20.OJP;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.ext.trias.mapping.ErrorMapper;
import org.opentripplanner.test.support.ResourceLoader;

class OjpToTriasTransformerTest {

  private static final ResourceLoader LOADER = ResourceLoader.of(OjpToTriasTransformerTest.class);
  private static final ZonedDateTime ZDT = ZonedDateTime.parse("2025-02-17T14:24:02+01:00");

  @ParameterizedTest
  @ValueSource(strings = { "stop-event-request.xml", "stop-event-request-coordinates.xml" })
  void stopEventRequest(String name) throws JAXBException, TransformerException {
    var triasReq = LOADER.fileToString(name);
    var transformed = OjpToTriasTransformer.triasToOjp(triasReq);
    var actual = toString(transformed);
    var file = LOADER.extTestResourceFile("../ojp/" + name);
    var original = readFile(file);
    writeFile(file, actual);
    assertFileEquals(original, file);
  }

  @Test
  void error() {
    var ojp = ErrorMapper.error("An error occurred", ZDT);
    var actual = OjpToTriasTransformer.ojpToTrias(ojp);
    var file = LOADER.extTestResourceFile("error.xml");
    var original = readFile(file);
    writeFile(file, actual);
    assertEqualStrings(original, actual);
  }

  private static void assertEqualStrings(String expected, String actual) {
    assertEquals(expected.strip(), actual.strip());
  }

  private static String toString(OJP ojpReq) throws JAXBException {
    var context = JAXBContext.newInstance(OJP.class);
    var marshaller = context.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    var writer = new StringWriter();
    marshaller.marshal(ojpReq, writer);

    return writer.getBuffer().toString();
  }
}

package org.opentripplanner.ext.ojp.resource;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The OJP and TRIAS endpoints take an XML document straight from an unauthenticated client, so a
 * document type declaration must never be resolved: an external entity would otherwise turn the
 * endpoints into a local file reader and an internal HTTP client (XXE).
 */
class SecureXmlTest {

  private static final String CANARY = "top-secret-xxe-canary";

  @TempDir
  static Path tempDir;

  private static String secretFileUri;

  @BeforeAll
  static void writeSecretFile() throws IOException {
    var secret = Files.writeString(tempDir.resolve("secret.txt"), CANARY);
    secretFileUri = secret.toUri().toString();
  }

  @Test
  void rejectTriasRequestWithExternalEntity() {
    var ex = assertThrows(TransformerException.class, () ->
      OjpToTriasTransformer.triasToOjp(triasRequestWithExternalEntity())
    );
    assertDoctypeRejected(ex);
  }

  @Test
  void rejectOjpRequestWithExternalEntity() {
    var ex = assertThrows(JAXBException.class, () ->
      OjpCodec.deserialize(ojpRequestWithExternalEntity())
    );
    assertDoctypeRejected(ex);
  }

  /**
   * The parser must fail on the declaration itself, and no part of the referenced file may show up
   * in what the caller gets back.
   */
  private static void assertDoctypeRejected(Exception ex) {
    var messages = causeMessages(ex);
    assertThat(messages.toString()).contains("DOCTYPE");
    assertThat(messages.toString()).doesNotContain(CANARY);
  }

  private static List<String> causeMessages(Throwable ex) {
    var messages = new ArrayList<String>();
    for (var t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
      messages.add(t.getMessage());
    }
    return messages;
  }

  private static String triasRequestWithExternalEntity() {
    return """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE Trias [<!ENTITY xxe SYSTEM "%s">]>
    <Trias version="1.2" xmlns="http://www.vdv.de/trias" xmlns:siri="http://www.siri.org.uk/siri">
      <ServiceRequest>
        <siri:RequestTimestamp>2025-01-27T13:34:00</siri:RequestTimestamp>
        <siri:RequestorRef>&xxe;</siri:RequestorRef>
        <RequestPayload>
          <StopEventRequest>
            <Location>
              <LocationRef>
                <StopPointRef>&xxe;</StopPointRef>
              </LocationRef>
            </Location>
          </StopEventRequest>
        </RequestPayload>
      </ServiceRequest>
    </Trias>
    """.formatted(secretFileUri);
  }

  private static String ojpRequestWithExternalEntity() {
    return """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE OJP [<!ENTITY xxe SYSTEM "%s">]>
    <OJP xmlns="http://www.vdv.de/ojp" xmlns:siri="http://www.siri.org.uk/siri">
      <siri:OJPRequest>
        <siri:ServiceRequest>
          <siri:RequestTimestamp>2025-01-27T13:34:00</siri:RequestTimestamp>
          <siri:RequestorRef>&xxe;</siri:RequestorRef>
        </siri:ServiceRequest>
      </siri:OJPRequest>
    </OJP>
    """.formatted(secretFileUri);
  }
}

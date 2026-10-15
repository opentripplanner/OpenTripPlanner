package org.opentripplanner.ext.ojp.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Factories for the XML machinery used by the OJP and TRIAS APIs, configured so that a document
 * received from a client can never make the server load anything from the file system or the
 * network.
 * <p>
 * The APIs accept an XML document in the request body. If that document is parsed with the JDK
 * defaults, a {@code DOCTYPE} declaring an external entity is resolved while parsing, which turns
 * the endpoint into a local file reader and an internal HTTP client (XXE). Neither the OJP nor the
 * TRIAS profile allows a document type declaration, so we reject one outright instead of merely
 * disabling entity resolution.
 * <p>
 * The default JDK implementations are requested explicitly: the feature names below are the ones
 * the JDK parsers understand, and a third-party parser on the classpath must not be able to
 * silently take over the request path.
 */
final class SecureXml {

  /**
   * Rejects a document type declaration instead of resolving it. Not a {@link XMLConstants}
   * constant, but understood by the JDK (Xerces-derived) parsers.
   */
  private static final String DISALLOW_DOCTYPE_DECL =
    "http://apache.org/xml/features/disallow-doctype-decl";

  private static final String EXTERNAL_GENERAL_ENTITIES =
    "http://xml.org/sax/features/external-general-entities";

  private static final String EXTERNAL_PARAMETER_ENTITIES =
    "http://xml.org/sax/features/external-parameter-entities";

  private SecureXml() {}

  /**
   * Wraps the given document in a source that is parsed without resolving anything outside the
   * document itself. Use this for every XML document that originates from a client.
   */
  static SAXSource source(String xml) {
    return source(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * @see #source(String)
   */
  static SAXSource source(InputStream xml) {
    try {
      // a SAXParserFactory is not documented as thread-safe, so it is not shared between requests
      var reader = saxParserFactory().newSAXParser().getXMLReader();
      return new SAXSource(reader, new InputSource(xml));
    } catch (ParserConfigurationException | SAXException e) {
      throw new IllegalStateException("Could not create a hardened XML reader.", e);
    }
  }

  /**
   * A factory for XSLT templates which is not allowed to reach outside the stylesheet it is given.
   */
  static TransformerFactory transformerFactory() {
    var factory = TransformerFactory.newDefaultInstance();
    enableSecureProcessing(factory);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  private static SAXParserFactory saxParserFactory() throws ParserConfigurationException {
    var factory = SAXParserFactory.newDefaultInstance();
    // the stylesheets match on namespaced elements, so the reader has to report namespaces
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    factory.setXIncludeAware(false);
    setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
    setFeature(factory, DISALLOW_DOCTYPE_DECL, true);
    setFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
    setFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
    return factory;
  }

  private static void setFeature(SAXParserFactory factory, String name, boolean value)
    throws ParserConfigurationException {
    try {
      factory.setFeature(name, value);
    } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
      throw new ParserConfigurationException(
        "The XML parser does not support the security feature '%s'.".formatted(name)
      );
    }
  }

  private static void enableSecureProcessing(TransformerFactory factory) {
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (TransformerConfigurationException e) {
      throw new IllegalStateException(
        "The XSLT implementation does not support secure processing.",
        e
      );
    }
  }
}

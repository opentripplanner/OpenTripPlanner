package org.opentripplanner.framework.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.Test;

class OtpHttpResponseTest {

  @Test
  void shouldProvideAccessToBody() throws Exception {
    String content = "test body content";
    InputStream body = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[0];

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    assertNotNull(response.body());
    assertEquals(content, new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
  }

  @Test
  void shouldProvideAccessToHeaders() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] {
      new BasicHeader("Content-Type", "application/json"),
      new BasicHeader("ETag", "\"12345\""),
    };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    Map<String, List<String>> responseHeaders = response.headers();
    assertNotNull(responseHeaders);
    assertEquals(2, responseHeaders.size());
    assertTrue(responseHeaders.containsKey("content-type"));
    assertTrue(responseHeaders.containsKey("etag"));
  }

  @Test
  void shouldHandleEmptyHeaders() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[0];

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    Map<String, List<String>> responseHeaders = response.headers();
    assertNotNull(responseHeaders);
    assertTrue(responseHeaders.isEmpty());
  }

  @Test
  void shouldProvideHeaderValueCaseInsensitive() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] {
      new BasicHeader("Content-Type", "application/json"),
      new BasicHeader("ETag", "\"12345\""),
    };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    assertTrue(response.header("Content-Type").isPresent());
    assertEquals("application/json", response.header("Content-Type").get());

    assertTrue(response.header("content-type").isPresent());
    assertEquals("application/json", response.header("content-type").get());

    assertTrue(response.header("CONTENT-TYPE").isPresent());
    assertEquals("application/json", response.header("CONTENT-TYPE").get());

    assertTrue(response.header("etag").isPresent());
    assertEquals("\"12345\"", response.header("etag").get());
  }

  @Test
  void shouldReturnEmptyOptionalForMissingHeader() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] { new BasicHeader("Content-Type", "application/json") };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    assertTrue(response.header("Missing-Header").isEmpty());
  }

  @Test
  void shouldHandleMultiValueHeaders() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] {
      new BasicHeader("Set-Cookie", "cookie1=value1"),
      new BasicHeader("Set-Cookie", "cookie2=value2"),
      new BasicHeader("Set-Cookie", "cookie3=value3"),
    };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    List<String> cookieValues = response.headerValues("Set-Cookie");
    assertNotNull(cookieValues);
    assertEquals(3, cookieValues.size());
    assertTrue(cookieValues.contains("cookie1=value1"));
    assertTrue(cookieValues.contains("cookie2=value2"));
    assertTrue(cookieValues.contains("cookie3=value3"));
  }

  @Test
  void shouldReturnFirstHeaderValueWhenMultipleExist() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] {
      new BasicHeader("Set-Cookie", "cookie1=value1"),
      new BasicHeader("Set-Cookie", "cookie2=value2"),
    };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    assertTrue(response.header("Set-Cookie").isPresent());
    assertEquals("cookie1=value1", response.header("Set-Cookie").get());
  }

  @Test
  void shouldReturnEmptyListForMissingHeaderValues() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] { new BasicHeader("Content-Type", "application/json") };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    List<String> values = response.headerValues("Missing-Header");
    assertNotNull(values);
    assertTrue(values.isEmpty());
  }

  @Test
  void shouldHandleHeaderValuesWithCaseInsensitivity() {
    InputStream body = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    Header[] headers = new Header[] {
      new BasicHeader("Accept-Encoding", "gzip"),
      new BasicHeader("Accept-Encoding", "deflate"),
    };

    OtpHttpResponse response = new OtpHttpResponse(body, headers, HttpStatus.SC_OK);

    List<String> values1 = response.headerValues("Accept-Encoding");
    List<String> values2 = response.headerValues("accept-encoding");
    List<String> values3 = response.headerValues("ACCEPT-ENCODING");

    assertEquals(2, values1.size());
    assertEquals(2, values2.size());
    assertEquals(2, values3.size());
  }
}

package org.opentripplanner.ext.legacygraphqlapi;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TestHeaders implements HttpHeaders {

  @Override
  public List<String> getRequestHeader(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHeaderString(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultivaluedMap<String, String> getRequestHeaders() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MediaType> getAcceptableMediaTypes() {
    return List.of(MediaType.APPLICATION_JSON_TYPE);
  }

  @Override
  public List<Locale> getAcceptableLanguages() {
    return List.of(Locale.ENGLISH);
  }

  @Override
  public MediaType getMediaType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Locale getLanguage() {
    return Locale.ENGLISH;
  }

  @Override
  public Map<String, Cookie> getCookies() {
    return Map.of();
  }

  @Override
  public Date getDate() {
    return Date.from(Instant.now());
  }

  @Override
  public int getLength() {
    return 0;
  }
}

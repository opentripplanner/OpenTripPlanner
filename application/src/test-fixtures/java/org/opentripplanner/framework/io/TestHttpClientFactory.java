package org.opentripplanner.framework.io;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.Mockito;

/**
 * Generally we avoid using Mockito in unit tests, but in this case we make an exception
 * because making actual HTTP calls or mocking the entire HTTP stack is even less pleasant.
 */
public class TestHttpClientFactory {

  public static OtpHttpClientFactory failingHttpFactory() {
    OtpHttpClientFactory clientFactory = Mockito.mock(OtpHttpClientFactory.class);
    OtpHttpClient otpHttpClient = Mockito.mock(OtpHttpClient.class);
    Mockito.when(clientFactory.create(any())).thenReturn(otpHttpClient);
    Mockito.when(otpHttpClient.getAndMapAsJsonNode(any(), any(), any())).thenThrow(
      OtpHttpClientException.class
    );
    return clientFactory;
  }
}

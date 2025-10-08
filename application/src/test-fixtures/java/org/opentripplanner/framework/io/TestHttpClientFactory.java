package org.opentripplanner.framework.io;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.Mockito;

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

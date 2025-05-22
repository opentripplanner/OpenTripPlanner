package org.opentripplanner.ext.interactivelauncher.api;

import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * Allow the interactive launcher intercept planing requests.
 */
public interface LauncherRequestDecorator {
  /**
   * The launcher may use this method to change the default plan request. Note! It is the DEFAULT
   * request which is passed in here, then the request-specific values are applied on top
   * of that.
   */
  RouteRequest intercept(RouteRequest defaultRequest);
}

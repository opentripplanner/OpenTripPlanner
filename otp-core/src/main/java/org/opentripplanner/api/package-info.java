/**
 * This package contains the code which exposes OpenTripPlanner services to the outside world as a
 * REST API. This includes Jersey REST resource classes (in the "resource" subpackage, picked up by
 * Jersey's package scanning process), and the classes modeling the structure of the response (in
 * the "model" subpackage). We provide the REST API as both a WAR-packaged servlet (via module
 * otp-rest-servlet) and a standalone Grizzly-based command-line invoked server (via module
 * otp-core).
 */
package org.opentripplanner.api;


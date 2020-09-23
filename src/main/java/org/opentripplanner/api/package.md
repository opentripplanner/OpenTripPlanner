# OTP REST API

This package contains the code which exposes OpenTripPlanner services to the outside world as a
REST API. This includes Jersey REST resource classes (in the "resource" subpackage, picked up by
Jersey's package scanning process), and the classes modeling the structure of the response (in
the "model" subpackage). We provide OTP with the REST API as a embedded standalone Grizzly-based 
command-line server.


# Scripting

## Introduction

The aim of OTP scripting is to be able to use and automate OTP from within scripts.

The available languages right now are *Jython* and *Groovy*; Jython being the most tested and supported. All the examples in this page are using Python. OTP Scripting could easily support in the future any other language that supports BSF (Bean Scripting Framework), such as _Javascript_ or _Ruby_.

__Note__: Jython is a Python-flavor using a Java JVM as runtime. Python and Jython are mostly compatible.

## Usage

There are currently 3 different modes for using scripting:

- Launching OTP with the `--script` command-line parameter, providing the script filename as an option.
- Starting an OTP server with the `--enableScriptingWebService` command-line parameter, and posting a script to execute to the `/ws/scripting/run` API end-point.
- Launching a Jython script with `otp-x.y.z-shaded.jar` in the classpath and creating the scripting entry-point bean in the script itself.

### Launching a script from OTP

The main advantage of this method is its simplicity. The drawback is that you need to start OTP everytime you run a script, which can be slow to startup for large graphs. The second drawback is that you can't import custom packages from within the script, you are limited to the "plain basic" Jython.

__Note__: The Jython (or Groovy) JAR are *not* included in the OTP shaded JAR. You thus have to add one of them (depending on your script) to the java classpath (see command below). Jython jar can be [downloaded here](http://www.jython.org/downloads.html). Make sure you select the "standalone" version of jython, otherwise some classes will be missing.

__Note__: Due a guava bug, there is an incompatibility between Jython 2.3 / 2.5 and OTP. To solve it, make sure otp.jar is added *before* jython-standalone.jar in the classpath. This bug should be solved in jython starting 2.7.

Start OTP specifying the classpath and adding the `--script` option:

```Bash
$ java -cp otp-x.y.z-shaded.jar:jython-standalone.jar org.opentripplanner.standalone.OTPMain --graphs . --script myscript.py
```

This will start OTP with a default graph in the current directory and execute the script `myscript.py`.

The return value of the script is discarded and printed to the log as a warning if the return value is not `null`.

### Scripting web-service

The main advantage of this method is that you do not need to start a new OTP server (which can take some time for large graphs) each time you start a new script. This mode is well-adapted for script development and debugging. The drawback is about security: it is not advisable to use this method for public-facing servers.

Start an OTP server as usual, adding the `--enableScriptingWebService` option:
``` Bash
$ java -cp otp-x.y.z-shaded.jar:jython-standalone.jar org.opentripplanner.standalone.OTPMain --graphs . --server --enableScriptingWebService
```
The API end-point `/ws/scripting/run` now accepts script content to be run, posted as multi-part form data.

To post a script to be executed, you can use either:

- The online form at `http://host:8080/scripting.html`, where you can upload a localfile to the server;
- Using `curl` from the command-line, such as:

```Bash
$ curl --form "scriptfile=@myscript.py" host:8080/otp/scripting/run
```

The return value of the script is passed as the return value of the web-service, in textual form. This can be used to return some value to the client (for example data in CSV form).

The standard output / error streams of the script (the place where the various `print` statements are printed)
is the standard output of the OTP server (the console where you start the OTP server).

The default location where files are loaded or saved is the working directory of the server, usually the directory from where you started the OTP server.

*Warning*: Enable this web-service on public-facing server is rather dangerous as this will open a whole range of exploits on the application.

### Using OTP as a library

The advantage of this method is its versatility. You can import any custom library in your script, and you are free to create the OTP server instance when you need it. The drawback is that you need to have an external Jython interpreter installed, and you need to startup OTP yourself within the script (2 lines of code, see below).

Example of use using python:

```Python
#!/usr/bin/jython
from org.opentripplanner.scripting.api import *
otp = OtpsEntryPoint.fromArgs([ "--graphs", "/path/to/graphs", "--router", "amsterdam" ])
# ... the rest of your script goes here ...
```

```Bash
$ java -cp otp-x.y.z-shaded.jar:jython-standalone.jar org.python.util.jython myscript.py
```

Or, simpler:

```Bash
$ jython -Dpython.path=otp-x.y.z-shaded.jar myscript.py
```

Note that contrary to java custom the jython "main" class is all lowercase, this is not a typo.

## Script tutorial

For a simple but functional example, please see [this script](https://github.com/opentripplanner/OpenTripPlanner/blob/master/src/test/resources/scripts/test.py).
With the embedded comments it should be self-explanatory.
In order to use this demo in the "library" mode, insert the 3 python lines given in the previous section at the beginning of the script.

## API

For a complete documentation, please see the [online JavaDoc](http://docs.opentripplanner.org/javadoc/master/) of all classes within the `org.opentripplanner.scripting.api` package.

Classes in this package are meant to be kept stable in time, at least regarding backward compatibility. Be careful as scripting is still in development phase; so there is no strong guarantee about the stability of the API, which is only a stated long-term goal. The aim is to achieve a stable API for the 1.0 OTP release.

OpenTripPlanner is migrating toward MkDocs for documentation generation.
This allows documentation to be stored directly within the OTP respository. Documentation changes can then be included in pull requests. The generated documentation is entirely static and will be served on readthedocs.org.

MkDocs is a Python program. See http://www.mkdocs.org/ for information on how to install it and how to generate a live local preview of the documentation while you're working on writing it.

In short:

$ pip install mkdocs
$ mkdocs serve


Tracking progress on documentation migration (many are wiki commit messages):
Move Building-OTP from wiki to mkdocs, replacing RST version.

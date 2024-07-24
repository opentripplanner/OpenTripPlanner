OpenTripPlanner uses MkDocs for documentation generation.
This allows documentation to be stored directly within the OTP repository.
Documentation changes can then be included in pull requests. The generated documentation is entirely static and will be served on readthedocs.org.

MkDocs is a Python program. See http://www.mkdocs.org/ for information on how to install it and how to generate a live local preview of the documentation while you're working on writing it.

In short, to preview the documentation as you work on it:

$ pip install -r docs/requirements.txt
$ mkdocs serve

If you create any new documentation pages, be sure to update the `nav:` section of `mkdocs.yml` in the root of the repository to ensure that your page is included in the table of contents and documentation navigation tree.

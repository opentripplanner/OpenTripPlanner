========================
Documentation Management
========================

Updated Solution, as of 11 December 2014
----------------------------------------

* ReadTheDocs http://docs.readthedocs.org/en/latest/index.html, now supports

* MkDocs http://www.mkdocs.org/, which uses

* Markdown http://daringfireball.net/projects/markdown/syntax, which is the format our documents are already in.

"MkDocs is a fast, simple and downright gorgeous static site generator that's geared towards building project documentation."

It is written in Python (+1) and configured with a single YAML configuration file (+1).


A summary of the current situation, as of 0.9.x
-----------------------------------------------

During pre-1.0 development OpenTripPlanner had three kinds of documents:

- The wiki on Github

- Javadoc built from source comments

- API Docs generated from source annotations by Enunciate

The wiki is generated automatically by Gollum via Github (not by Jekyll as I previously stated). It it managed as a Github repo, and Github also provides a simple web-based front end for editing it. Javadoc and API doc generation were manually triggered by the 'mvn site' goal, which leaves HTML docs in the target/ directories after the build. These were then manually copied to the web server.

The wiki is not particularly well organized or indexed, and it's never clear which version of the software the pages refer to. Starting with version 1.0 we want to:

1. Version-control the documentation that was previously in the wiki.

2. Snapshot the documentation at every release and keep old versions available.

3. Preferably generate indexes and tables of contents to facilitate navigating the docs.

By placing the wiki pages inside the OTP repo itself, we can include documentation changes in pull requests and easily see which version of the docs corresponds with which version of the code. Unfortunately Github pages will not serve up a set of Jekyll files in a subdirectory of your repo. You must put them at the root level (which implies a separate repo from the code to avoid clutter) or in a totally different, initially empty branch (which prevents you from combining code and docs in pull requests). I confirmed with Github support that this is the case. Therefore automated Github hosting is not an option. 

Hosting and commit-driven generation
------------------------------------

We can either generate all the docs (including the former wiki) ourselves manually, do it as part of the CI build process, or use a service that watches the repo and generates docs upon commits.

Automatic, hosted solutions include http://www.hasdocs.com/ and https://readthedocs.org/. I have had recommendations for both of these.  Hasdocs is "polyglot" and will build Jekyll (HTML, Textile, Markdown, and Liquid), Sphinx docs, and Javadoc. Readthedocs appears to concentrate entirely on Sphinx documentation.

Documentation markup and generation packages
--------------------------------------------

Our options for writing the docs include Markdown (supported by Gollum and Jekyll) and Sphinx.

We are currently using Github's Gollum wiki engine, which is based on Markdown. Jekyll can also use markdown, so the conversion would be rather easy. However, Jekyll has somewhat of a date-stamped blog-post orientation, and doesn't seem ideal for docs. One advantage of the Github wiki is that it allows anyone to make small improvements and lowers the barrier to contribution to OTP. However, in practice the wiki is almost entirely edited by developers who will be comfortable with a text editor and Git.

Sphinx is intended specifically for software documentation, and is used to generate the documents for Python (the language itself, as well as many libraries and projects in Python). It uses reStructuredText, which is not a big leap from Markdown. It includes the ability to pull comments out of Python code and reference functions in the docs, so it does have a privileged relationship with Python, but is widely used to produce documentation for projects written in other languages. Sphinx produces tables of contents etc. and seems somewhat more sophisticated than Gollum.

Some other options that came up in searches:

- http://beautifuldocs.com/

- http://daux.io/ (requires PHP, eek)


Solution
--------

I am leaning toward:

* ReadTheDocs http://docs.readthedocs.org/en/latest/index.html, which uses

* Sphinx http://sphinx-doc.org/, which uses

* RestructuredText http://docutils.sourceforge.net/docs/ref/rst/restructuredtext.html

You can experiment with Restructured Text here: http://rst.ninjs.org/

In case we don't want to use a documentation build/hosting service like readthedocs, there is a Maven plugin to run Sphinx as part of the site generation (when we also build Javadoc): http://tomdz.github.io/sphinx-maven/

From there it would just take a small script or following a simple reminder HOWTO to upload the docs. Maven does provide the option of automatically deploying the site/docs, but as with many other things in Maven it's prone to mind-numbing debugging sessions, deciphering undocumented defaults and sifting through pages of XML.

Note that Jenkins does allow running additional commands or goals upon completion of each build. So the CI server would also be an appropriate place to generate docs and push them to a web server, possibly even using some Maven automation (though more likely a simple script to avoid suffering).

Web hosting of manually generated docs
--------------------------------------

The Markdown/Sphinx-produced docs might be hosted by a documentation service, but the Javadoc or API docs very well might not. Which means we need another place to host them, along with testing resources such as OSM and GTFS, pre-built JAR files for each commit/release, and other OTP miscellanea. We are already running a CI server which also contains a stock, simple Nginx serving up that static content. It's particularly convenient because the CI server can just copy or link docs and JARs to another directory, rather than moving them around the network with SSH keys and such. It does entail a small amount of maintenance, but this is very minimal: the uptime is now at 250-something days, and the server is running anyway for CI and testing. The alternative is to use Amazon buckets or the like, but I don't know if I want to mess with automated copying of resources to Amazon on every commit.

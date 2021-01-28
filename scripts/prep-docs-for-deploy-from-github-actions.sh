# build markdown docs using mkdocs
mkdocs build
# build enunciate and JavaDoc
mvn -DskipTests site
# copy enunciate API docs into mkdocs folder
cp -R target/site/enunciate/apidocs target/mkdocs/api
# copy JavaDoc into mkdocs folder
cp -R target/apidocs target/mkdocs/JavaDoc

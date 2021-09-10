# build markdown docs using mkdocs
mkdocs build
# build enunciate and JavaDoc
mvn -DskipTests site
mkdir -p target/mkdocs/api
mkdir -p target/mkdocs/JavaDoc
# copy enunciate API docs into mkdocs folder
cp -R target/site/enunciate/apidocs target/mkdocs/api
# copy JavaDoc into mkdocs folder
cp -R target/apidocs target/mkdocs/JavaDoc

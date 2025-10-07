package org.opentripplanner.datastore.base;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * This is a simple {@link CompositeDataSource} using a list of children. It is usefull
 * for testing.
 */
public class ListCompositeDataSource implements CompositeDataSource {

  private static final String URI_DELIMITER = "/";
  private final String name;
  private final FileType type;
  private final List<DataSource> children;

  public ListCompositeDataSource(String name, FileType type, List<DataSource> children) {
    this.name = name;
    this.type = type;
    this.children = children;
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public Collection<DataSource> content() {
    return children;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public DataSource entry(String name) {
    return children.stream().filter(it -> name.equals(it.name())).findFirst().orElse(null);
  }

  @Override
  public void close() {
    /* Nothing to close */
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String path() {
    return getClass().getSimpleName() + URI_DELIMITER + System.identityHashCode(this);
  }

  @Override
  public URI uri() {
    return URI.create(path() + URI_DELIMITER + name);
  }

  @Override
  public FileType type() {
    return type;
  }
}

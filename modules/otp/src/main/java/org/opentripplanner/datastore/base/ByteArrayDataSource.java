package org.opentripplanner.datastore.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * This data source keep its data in memory as a byte array. You may insert data using {@link
 * #withBytes(byte[])} or {@link #asOutputStream()}. To access data you can use the {@link
 * #asBytes()} or reading the input stream {@link #asInputStream()}.
 * <p>
 * Any existing data in the datasource will be erased if you insert data using the output stream
 * {@link #asOutputStream()} or set the byte array {@link #withBytes(byte[])}.
 */
public class ByteArrayDataSource implements DataSource {

  private final String path;
  private final String name;
  private final FileType type;
  private final long size;
  private final long lastModified;
  private final boolean writable;
  private ByteArrayOutputStream out = null;
  private byte[] buffer;

  public ByteArrayDataSource(
    String path,
    String name,
    FileType type,
    long size,
    long lastModified,
    boolean writable
  ) {
    this.path = path;
    this.name = name;
    this.type = type;
    this.size = size;
    this.lastModified = lastModified;
    this.writable = writable;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public URI uri() {
    return URI.create(path());
  }

  @Override
  public FileType type() {
    return type;
  }

  @Override
  public long size() {
    return size;
  }

  @Override
  public long lastModified() {
    return lastModified;
  }

  @Override
  public boolean isWritable() {
    return writable;
  }

  /**
   * Return the internal byte array as an {@link InputStream}. A new input stream is generated for
   * each call to this method, and it is safe to do so.
   */
  @Override
  public InputStream asInputStream() {
    return new ByteArrayInputStream(asBytes());
  }

  @Override
  public byte[] asBytes() {
    return buffer == null ? out.toByteArray() : buffer;
  }

  /**
   * Clean any existing data, and return a new {@link OutputStream} which can be used to insert data
   * into the byte array.
   * <p>
   * If the source is created with {@code writable = false} then this method will
   * throw an exception, instead use the {@link #withBytes(byte[])} to inject data.
   */
  @Override
  public OutputStream asOutputStream() {
    if (!writable) {
      throw new UnsupportedOperationException(
        "This datasource type " + type() + " do not support WRITING. Can not write to: " + path()
      );
    }

    if (out == null) {
      out = new ByteArrayOutputStream(1024);
      buffer = null;
    }
    return out;
  }

  @Override
  public String toString() {
    return type + " " + path();
  }

  /**
   * Clean any existing data, and set the byte array.
   */
  public ByteArrayDataSource withBytes(byte[] buffer) {
    this.buffer = buffer;
    this.out = null;
    return this;
  }
}

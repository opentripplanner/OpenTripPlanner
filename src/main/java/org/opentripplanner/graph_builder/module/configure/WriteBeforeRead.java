package org.opentripplanner.graph_builder.module.configure;

/**
 * This class allow a data element to be written into this class and then fetched later, or
 * id no write operation occour - the default value is returned. If the write operation happens
 * after the read, an exception is thrown. This ensure that the write happens before the read.
 */
class WriteBeforeRead<T> {

  /**
   * The data element is not set or read.
   */
  private boolean open = true;
  private T value;

  private WriteBeforeRead(T defaultData) {
    this.value = defaultData;
  }

  static <T> WriteBeforeRead<T> of(T defaultValue) {
    return new WriteBeforeRead<>(defaultValue);
  }

  T read() {
    open = false;
    return value;
  }

  void write(T value) {
    if (open) {
      this.value = value;
      this.open = false;
    } else {
      throw new IllegalStateException("The 'write' operation happend after the 'read' operation.");
    }
  }
}

package org.opentripplanner.transit.model.framework;

/**
 * This exception is used by the main OTP business logic to signal that one of the
 * ids passed in is not found. This exception should be handled in a generic way in each
 * API.
 * <p>
 * When an entity is not found, it indicates that there is a system integration error. This
 * should not be used if the user type in the id, then the client should validate the id
 * before it is passed into OTP.
 */
public class EntityNotFoundException extends RuntimeException {

  private final String entityName;
  private final FeedScopedId id;

  /**
   * Use this if the id can be of more than one type, or you would like to provide an
   * alternative name.
   */
  public EntityNotFoundException(String entityName, FeedScopedId id) {
    this.entityName = entityName;
    this.id = id;
  }

  public EntityNotFoundException(Class<?> entityType, FeedScopedId id) {
    this(entityType.getSimpleName(), id);
  }

  @Override
  public String getMessage() {
    return entityName + " entity not found: " + id;
  }
}

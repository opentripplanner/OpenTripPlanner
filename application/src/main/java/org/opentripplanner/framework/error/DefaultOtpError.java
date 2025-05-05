package org.opentripplanner.framework.error;

class DefaultOtpError implements OtpError {

  private final String errorCode;
  private final String messageTemplate;
  private final Object[] messageArguments;

  public DefaultOtpError(String errorCode, String messageTemplate, Object... messageArguments) {
    this.errorCode = errorCode;
    this.messageTemplate = messageTemplate;
    this.messageArguments = messageArguments;
  }

  @Override
  public String errorCode() {
    return errorCode;
  }

  @Override
  public String messageTemplate() {
    return messageTemplate;
  }

  @Override
  public Object[] messageArguments() {
    return messageArguments;
  }

  @Override
  public String toString() {
    return errorCode + "(" + message() + ")";
  }
}

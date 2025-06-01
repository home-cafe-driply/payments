package com.driply.payments.payment.exception;

import lombok.Getter;

@Getter
public class TossApiException extends RuntimeException {
  private final String responseBody;

  public TossApiException(String message, String responseBody) {
    super(message + " | Response: " + responseBody);
    this.responseBody = responseBody;
  }

}

package com.github.skoved.uniqueemails;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handles IllegalArgumentExceptions thrown by the validateEmails method of EmailController.
 */
@ControllerAdvice
public class IllegalArgumentAdvice {

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String illegalArgumentExceptionHandler(IllegalArgumentException ex) {
        return ex.getMessage() + " Please submit properly formed email addresses."
                + " https://en.wikipedia.org/wiki/Email_address#Examples has"
                + " examples of valid email addresses.";
    }
}

package com.example.hotelreservationsystem.exception;

import com.example.hotelreservationsystem.dto.ErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        var error = ErrorResponse.builder()
                                 .error("Registration Failed")
                                 .message(ex.getMessage())
                                 .status(HttpStatus.BAD_REQUEST.value())
                                 .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        var error = ErrorResponse.builder()
                                 .error("Authentication Failed")
                                 .message(ex.getMessage())
                                 .status(HttpStatus.UNAUTHORIZED.value())
                                 .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        var error = ErrorResponse.builder()
                                 .error("User Not Found")
                                 .message(ex.getMessage())
                                 .status(HttpStatus.NOT_FOUND.value())
                                 .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        var error = ErrorResponse.builder()
                                 .error("Internal Server Error")
                                 .message(ex.getMessage())
                                 .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                 .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        var errorMessages = ex.getBindingResult()
                              .getAllErrors()
                              .stream()
                              .map(DefaultMessageSourceResolvable::getDefaultMessage)
                              .toList();

        var message = String.join("; ", errorMessages);

        var error = ErrorResponse.builder()
                                 .error("Bad Request")
                                 .message(message)
                                 .status(HttpStatus.BAD_REQUEST.value())
                                 .build();
        return ResponseEntity.badRequest().body(error);
    }
}

package com.vantage.core.exception;

import com.vantage.inventory.app.InventoryConflictException;
import com.vantage.payment.app.IdempotencyConflictException;
import com.vantage.core.ratelimiter.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class ProblemDetailFactory {

    private ProblemDetailFactory() {}

    public static ProblemDetail createInventoryConflict(InventoryConflictException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://vantage.io/errors/inventory-conflict"));
        pd.setTitle("Inventory Conflict");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        // Extract expected and current version from exception message? We'll need to pass them.
        // Since the exception currently only has message, we need to enrich it.
        // We'll parse the message: "Version mismatch. Expected: X, Actual: Y"
        String msg = ex.getMessage();
        Long expected = null;
        Long current = null;
        if (msg != null && msg.contains("Expected:") && msg.contains("Actual:")) {
            try {
                String[] parts = msg.split("Expected:")[1].split(",");
                expected = Long.parseLong(parts[0].trim());
                String actualPart = parts[1].split("Actual:")[1].trim();
                current = Long.parseLong(actualPart);
            } catch (Exception ignored) {}
        }
        if (expected != null) {
            pd.setProperty("expectedVersion", expected);
        }
        if (current != null) {
            pd.setProperty("currentVersion", current);
        }
        return pd;
    }

    public static ProblemDetail createIdempotencyConflict(IdempotencyConflictException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://vantage.io/errors/idempotency-conflict"));
        pd.setTitle("Idempotency Conflict");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setType(URI.create("https://vantage.io/errors/rate-limit-exceeded"));
        pd.setTitle("Rate Limit Exceeded");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("retryAfter", ex.getRetryAfterSeconds());
        return pd;
    }

    public static ProblemDetail createResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Not Found");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createBadRequest(IllegalArgumentException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Bad Request");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Validation Error");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        pd.setProperty("errors", errors);
        return pd;
    }

    public static ProblemDetail createGeneric(Exception ex, HttpStatus status, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}

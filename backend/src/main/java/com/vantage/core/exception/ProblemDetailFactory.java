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

    private static URI safeInstance(WebRequest request) {
        String desc = request.getDescription(false);
        String instance = desc.startsWith("uri=") ? desc.substring(4) : desc;
        try {
            return URI.create(instance);
        } catch (IllegalArgumentException e) {
            return URI.create("/");
        }
    }

    public static ProblemDetail createInventoryConflict(InventoryConflictException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://vantage.io/errors/inventory-conflict"));
        pd.setTitle("Inventory Conflict");
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        if (ex.getExpectedVersion() != null) {
            pd.setProperty("expectedVersion", ex.getExpectedVersion());
        }
        if (ex.getCurrentVersion() != null) {
            pd.setProperty("currentVersion", ex.getCurrentVersion());
        }
        return pd;
    }

    public static ProblemDetail createIdempotencyConflict(IdempotencyConflictException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://vantage.io/errors/idempotency-conflict"));
        pd.setTitle("Idempotency Conflict");
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createRateLimitExceeded(RateLimitExceededException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setType(URI.create("https://vantage.io/errors/rate-limit-exceeded"));
        pd.setTitle("Rate Limit Exceeded");
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("retryAfter", ex.getRetryAfterSeconds());
        return pd;
    }

    public static ProblemDetail createResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Not Found");
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createBadRequest(IllegalArgumentException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Bad Request");
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Validation Error");
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        pd.setProperty("errors", errors);
        return pd;
    }

    public static ProblemDetail createVantageDomainException(VantageDomainException ex, WebRequest request) {
        String type = "https://vantage.io/errors/" + ex.getClass().getSimpleName().toLowerCase().replace("exception", "");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setType(URI.create(type));
        pd.setTitle(ex.getClass().getSimpleName());
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    public static ProblemDetail createGeneric(Exception ex, HttpStatus status, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setType(URI.create("about:blank"));
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(safeInstance(request));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}

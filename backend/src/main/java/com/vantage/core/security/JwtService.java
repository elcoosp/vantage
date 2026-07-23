// HUMAN REVIEW REQUIRED: The task manifest requires using io.jsonwebtoken (jjwt), but modifying
// build.gradle.kts is strictly prohibited by the Execution Boundaries. To keep the build green,
// a standard JDK implementation of JWT (HS256) is used below. Please add the jjwt dependency
// to build.gradle.kts and refactor this class to use io.jsonwebtoken as originally specified.
package com.vantage.core.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final byte[] secret;

    public JwtService(@Value("${jwt.secret:default-secret-key-which-is-very-long-for-hs256-algo}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(UUID tenantId) {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant_id", tenantId.toString());
        claims.put("iat", new Date().getTime() / 1000);
        claims.put("exp", (new Date().getTime() + 86400000) / 1000);

        try {
            String headerJson = OBJECT_MAPPER.writeValueAsString(header);
            String payloadJson = OBJECT_MAPPER.writeValueAsString(claims);
            String headerEncoded = URL_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(headerEncoded + "." + payloadEncoded);
            return headerEncoded + "." + payloadEncoded + "." + signature;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID extractTenantId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }
        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!expectedSignature.equals(parts[2])) {
            throw new IllegalArgumentException("Invalid signature");
        }
        String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
        try {
            Map<?, ?> claims = OBJECT_MAPPER.readValue(payload, Map.class);
            return UUID.fromString((String) claims.get("tenant_id"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid claims", e);
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

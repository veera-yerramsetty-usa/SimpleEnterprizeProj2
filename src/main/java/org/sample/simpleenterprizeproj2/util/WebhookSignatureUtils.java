package org.sample.simpleenterprizeproj2.util;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookSignatureUtils {

    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSignatureUtils() {}

    public static String computeSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
        }
    }
}

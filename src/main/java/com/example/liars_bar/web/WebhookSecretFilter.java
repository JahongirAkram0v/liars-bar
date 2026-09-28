package com.example.liars_bar.web;

import com.example.liars_bar.config.TelegramProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Webhook so'rovini body o'qilishidan oldin tekshiradi: faqat Telegram yuborgan
 * (to'g'ri X-Telegram-Bot-Api-Secret-Token sarlavhali) va hajmi cheklangan so'rovlar o'tadi.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebhookSecretFilter extends OncePerRequestFilter {

    static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";
    private static final long MAX_BODY_BYTES = 256 * 1024;

    private final String webhookPath;
    private final byte[] secret;

    public WebhookSecretFilter(TelegramProperties properties) {
        this.webhookPath = properties.webhookPath();
        this.secret = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !webhookPath.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(SECRET_HEADER);
        if (header == null || !MessageDigest.isEqual(secret, header.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        chain.doFilter(request, response);
    }
}

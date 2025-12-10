package com.example.liars_bar.model;

import java.util.Map;

public record Response(Map<String, Object> body, String method) {}

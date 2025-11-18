package com.example.liars_bar.model;

import java.util.List;
import java.util.Map;

public record Result(List<List<Map<String, Object>>> keyboard, String text) {}
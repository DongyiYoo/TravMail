package com.travmail.travmail.Tagging;

import java.util.EnumMap;
import java.util.List;

public record Result(
                Category category,
                int bestScore,
                EnumMap<Category, Integer> scoreMap,
                List<String> evidence) {
}
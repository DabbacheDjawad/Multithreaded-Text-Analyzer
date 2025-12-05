package com.example.text_analyzer;

import java.util.*;
import java.util.regex.*;

public class TextAnalyzer {

    public static Map<String, String> analyze(String text) {

        Map<String, String> result = new LinkedHashMap<>();

        if (text == null || text.isBlank()) {
            result.put("Total Words", "0");
            result.put("Unique Words", "0");
            result.put("Most Frequent Word", "-");
            result.put("Characters (with spaces)", "0");
            result.put("Characters (no spaces)", "0");
            result.put("Sentence Count", "0");
            result.put("Average Word Length", "0");
            result.put("Longest Word", "-");
            result.put("Shortest Word", "-");
            result.put("Top 5 Words", "-");
            return result;
        }

        String[] tokens = text.toLowerCase().split("\\W+");

        // WORD COUNTS
        int totalWords = tokens.length;
        result.put("Total Words", String.valueOf(totalWords));

        Set<String> uniqueSet = new HashSet<>(Arrays.asList(tokens));
        result.put("Unique Words", String.valueOf(uniqueSet.size()));

        // FREQUENCY MAP
        Map<String, Integer> freq = new HashMap<>();
        for (String w : tokens) {
            if (!w.isBlank()) {
                freq.put(w, freq.getOrDefault(w, 0) + 1);
            }
        }

        // MOST FREQUENT WORD
        String maxWord = "-";
        int maxCount = 0;
        for (var e : freq.entrySet()) {
            if (e.getValue() > maxCount) {
                maxWord = e.getKey();
                maxCount = e.getValue();
            }
        }
        result.put("Most Frequent Word", maxWord + " (" + maxCount + ")");

        // CHARACTERS
        result.put("Characters (with spaces)", String.valueOf(text.length()));
        result.put("Characters (no spaces)", String.valueOf(text.replace(" ", "").length()));

        // SENTENCE COUNT
        String[] sentences = text.split("[.!?]+");
        result.put("Sentence Count", String.valueOf(sentences.length));

        // AVERAGE WORD LENGTH
        int totalLength = Arrays.stream(tokens).mapToInt(String::length).sum();
        double avgLen = totalWords == 0 ? 0 : (double) totalLength / totalWords;
        result.put("Average Word Length", String.format("%.2f", avgLen));

        // LONGEST / SHORTEST WORD
        String longest = "-", shortest = "-";
        for (String w : tokens) {
            if (!w.isBlank()) {
                if (longest.equals("-") || w.length() > longest.length()) longest = w;
                if (shortest.equals("-") || w.length() < shortest.length()) shortest = w;
            }
        }

        result.put("Longest Word", longest);
        result.put("Shortest Word", shortest);

        // TOP 5 WORDS
        String top5 =
                freq.entrySet()
                        .stream()
                        .sorted((a, b) -> b.getValue() - a.getValue())
                        .limit(5)
                        .map(e -> e.getKey() + " (" + e.getValue() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("-");

        result.put("Top 5 Words", top5);

        return result;
    }
}

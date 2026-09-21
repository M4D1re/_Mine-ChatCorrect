package com.kaiki.minechatcorrect.spell;

import com.kaiki.minechatcorrect.config.DictionaryManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class SpellChecker {

    private Set<String> dictionary;

    private String cachedText;
    private List<MisspelledWord> cachedMisspellings = List.of();

    private String cachedSuggestionWord;
    private List<String> cachedSuggestions = List.of();

    private static final Pattern WORD_PATTERN =
            Pattern.compile("[A-Za-zА-Яа-яЁё][A-Za-zА-Яа-яЁё']{2,}");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(https?://\\S+|www\\.\\S+|\\b[a-z0-9.-]+\\.[a-z]{2,}\\S*)");

    private final DictionaryManager dictionaryManager;

    public SpellChecker(Path configDir) {
        this.dictionaryManager = new DictionaryManager(configDir);
        this.dictionary = dictionaryManager.allWords();
    }

    SpellChecker(Set<String> dictionary) {
        this.dictionaryManager = null;
        this.dictionary = new LinkedHashSet<>(dictionary);
    }

    private List<MisspelledWord> calculateMisspellings(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (text.startsWith("/")) {
            return List.of();
        }

        ArrayList<MisspelledWord> results = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        List<int[]> urlRanges = urlRanges(text);

        while (matcher.find()) {
            String rawWord = matcher.group();

            if (shouldIgnore(rawWord) || isInsideRange(matcher.start(), matcher.end(), urlRanges)) {
                continue;
            }

            String normalized = normalize(rawWord);
            if (!dictionary.contains(normalized)) {
                results.add(new MisspelledWord(rawWord, matcher.start(), matcher.end()));
            }
        }

        return results;
    }

    public List<MisspelledWord> findMisspellings(String text) {
        String currentText = text == null ? "" : text;

        if (currentText.equals(cachedText)) {
            return cachedMisspellings;
        }

        List<MisspelledWord> result =
                List.copyOf(calculateMisspellings(currentText));

        cachedText = currentText;
        cachedMisspellings = result;
        return result;
    }

    public List<String> suggestionsFor(String word) {
        String normalized = word == null ? "" : normalize(word);

        if (normalized.equals(cachedSuggestionWord)) {
            return cachedSuggestions;
        }
        List<String> result =
                List.copyOf(calculateSuggestions(normalized));
        cachedSuggestionWord = normalized;
        cachedSuggestions = result;
        return result;
    }

    private List<String> calculateSuggestions(String word) {
        String normalized = normalize(word);
        if (normalized.isBlank()) {
            return List.of();
        }

        return dictionary.stream()
                .filter(candidate -> Math.abs(candidate.length() - normalized.length()) <= 2)
                .map(candidate -> new Suggestion(candidate, distance(normalized, candidate)))
                .filter(suggestion -> suggestion.distance() <= Math.max(2, normalized.length() / 3))
                .sorted(Comparator.comparingInt(Suggestion::distance).thenComparing(Suggestion::word))
                .limit(8)
                .map(Suggestion::word)
                .toList();
    }

    public String bestSuggestionFor(String word) {
        List<String> suggestions = suggestionsFor(word);
        return suggestions.isEmpty() ? "" : suggestions.getFirst();
    }

    public DictionaryManager dictionaryManager() {
        if (dictionaryManager == null) {
            throw new IllegalStateException("This SpellChecker was created with an in-memory test dictionary.");
        }
        return dictionaryManager;
    }

    public void clearCaches() {
        cachedText = null;
        cachedMisspellings = List.of();

        cachedSuggestionWord = null;
        cachedSuggestions = List.of();
    }

    private void refreshDictionary() {
        dictionary = requireDictionaryManager().allWords();
        clearCaches();
    }

    public void addWord(String word) {
        requireDictionaryManager().addExtraWord(word);
        reloadDictionaries();
    }

    public void reloadDictionaries() {
        requireDictionaryManager().reload();
        refreshDictionary();
    }

    public String importDictionary(String source) throws IOException {
        String name = requireDictionaryManager().importDictionary(source);
        refreshDictionary();
        return name;
    }

    public void setDictionaryEnabled(String name, boolean enabled) {
        requireDictionaryManager().setDictionaryEnabled(name, enabled);
        refreshDictionary();
    }

    public void removeDictionary(String name) {
        requireDictionaryManager().removeDictionary(name);
        refreshDictionary();
    }

    private DictionaryManager requireDictionaryManager() {
        if (dictionaryManager == null) {
            throw new IllegalStateException("This SpellChecker was created with an in-memory test dictionary.");
        }
        return dictionaryManager;
    }

    private boolean shouldIgnore(String word) {
        if (word.length() < 3) {
            return true;
        }

        return word.indexOf('_') >= 0 || word.indexOf(':') >= 0 || word.indexOf('@') >= 0;
    }

    private List<int[]> urlRanges(String text) {
        ArrayList<int[]> ranges = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            ranges.add(new int[]{matcher.start(), matcher.end()});
        }
        return ranges;
    }

    private boolean isInsideRange(int start, int end, List<int[]> ranges) {
        for (int[] range : ranges) {
            if (start >= range[0] && end <= range[1]) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String word) {
        String normalized = word.toLowerCase(Locale.ROOT);

        if (normalized.endsWith("'s")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }

        return normalized;
    }

    private int distance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[right.length()];
    }

    private record Suggestion(String word, int distance) {
    }
}

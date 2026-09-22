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
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;


public final class SpellChecker {

//     private static final int MAX_SUGGESTIONS = 8;
//
//     private static final Comparator<Suggestion> SUGGESTION_ORDER =
//             Comparator.comparingInt(Suggestion::distance)
//                     .thenComparing(Suggestion::word);
//
//     private Map<Integer, List<String>> wordsByLength = Map.of();
//
    private Set<String> dictionary;

    private String cachedText;
    private List<MisspelledWord> cachedMisspellings = List.of();

//     private String cachedSuggestionWord;
//     private List<String> cachedSuggestions = List.of();
//
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[A-Za-zА-Яа-яЁё][A-Za-zА-Яа-яЁё']{2,}");
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)(https?://\\S+|www\\.\\S+|\\b[a-z0-9.-]+\\.[a-z]{2,}\\S*)");

    private final DictionaryManager dictionaryManager;

    public SpellChecker(Path configDir) {
        this.dictionaryManager = new DictionaryManager(configDir);
        this.dictionary = dictionaryManager.allWords();
        // rebuildDictionaryIndex();
    }

    SpellChecker(Set<String> dictionary) {
        this.dictionaryManager = null;
        this.dictionary = new LinkedHashSet<>(dictionary);
        // rebuildDictionaryIndex();
    }

//     private void rebuildDictionaryIndex() {
//         Map<Integer, List<String>> index = new HashMap<>();
//
//         for (String word : dictionary) {
//             index.computeIfAbsent(
//                     word.length(),
//                     length -> new ArrayList<>()
//             ).add(word);
//         }
//
//         index.replaceAll((length, words) -> List.copyOf(words));
//         wordsByLength = Map.copyOf(index);
//     }
//
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

//     public List<String> suggestionsFor(String word) {
//         String normalized = word == null ? "" : normalize(word);
//
//         if (normalized.equals(cachedSuggestionWord)) {
//             return cachedSuggestions;
//         }
//         List<String> result =
//                 List.copyOf(calculateSuggestions(normalized, wordsByLength));
//         cachedSuggestionWord = normalized;
//         cachedSuggestions = result;
//         return result;
//     }
//
//     /**
//      * Call on the client thread. The returned task searches a captured immutable
//      * index and can run on a worker thread without reading or changing caches.
//      */
//     public Supplier<List<String>> prepareSuggestionSearch(String word) {
//         String normalized = word == null ? "" : normalize(word);
//         Map<Integer, List<String>> snapshot = wordsByLength;
//         return () -> calculateSuggestions(normalized, snapshot);
//     }
//
//     private List<String> calculateSuggestions(String normalized, Map<Integer, List<String>> index) {
//         if (normalized.isBlank()) {
//             return List.of();
//         }
//
//         int maxDistance = Math.max(2, normalized.length() / 3);
//
//         PriorityQueue<Suggestion> best = new PriorityQueue<>(
//                 MAX_SUGGESTIONS,
//                 SUGGESTION_ORDER.reversed()
//         );
//
//         int[] previous = new int[normalized.length() + 1];
//         int[] current = new int[normalized.length() + 1];
//
//         int minLength = Math.max(0, normalized.length() - 2);
//         int maxLength = normalized.length() + 2;
//
//         for (int length = minLength; length <= maxLength; length++) {
//             List<String> candidates = index.get(length);
//             if (candidates == null) {
//                 continue;
//             }
//
//             for (String candidate : candidates) {
//                 if (Thread.currentThread().isInterrupted()) {
//                     throw new CancellationException("Suggestion search cancelled");
//                 }
//                 int cutoff = maxDistance;
//
//                 if (best.size() == MAX_SUGGESTIONS) {
//                     cutoff = Math.min(cutoff, best.peek().distance());
//                 }
//
//                 int candidateDistance = distance(
//                         candidate,
//                         normalized,
//                         cutoff,
//                         previous,
//                         current
//                 );
//
//                 if (candidateDistance > cutoff) {
//                     continue;
//                 }
//
//                 if (best.size() == MAX_SUGGESTIONS) {
//                     Suggestion worst = best.peek();
//
//                     boolean isBetter =
//                             candidateDistance < worst.distance()
//                                     || (candidateDistance == worst.distance()
//                                     && candidate.compareTo(worst.word()) < 0);
//
//                     if (!isBetter) {
//                         continue;
//                     }
//
//                     best.poll();
//                 }
//
//                 best.offer(new Suggestion(candidate, candidateDistance));
//             }
//         }
//
//         return best.stream()
//                 .sorted(SUGGESTION_ORDER)
//                 .map(Suggestion::word)
//                 .toList();
//     }
//
//     public String bestSuggestionFor(String word) {
//         List<String> suggestions = suggestionsFor(word);
//         return suggestions.isEmpty() ? "" : suggestions.getFirst();
//     }
//
    public DictionaryManager dictionaryManager() {
        if (dictionaryManager == null) {
            throw new IllegalStateException("This SpellChecker was created with an in-memory test dictionary.");
        }
        return dictionaryManager;
    }

    public void clearCaches() {
        cachedText = null;
        cachedMisspellings = List.of();

        // cachedSuggestionWord = null;
        // cachedSuggestions = List.of();
    }

    private void refreshDictionary() {
        dictionary = requireDictionaryManager().allWords();
        // rebuildDictionaryIndex();
        clearCaches();
    }

    public void addWord(String word) {
        requireDictionaryManager().addExtraWord(word);
        refreshDictionary();
    }

    public void removeWord(String word) {
        requireDictionaryManager().removeExtraWord(word);
        refreshDictionary();
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

//     private int distance(
//             String left,
//             String right,
//             int maxDistance,
//             int[] previous,
//             int[] current
//     ) {
//         if (Math.abs(left.length() - right.length()) > maxDistance) {
//             return maxDistance + 1;
//         }
//
//         for (int j = 0; j <= right.length(); j++) {
//             previous[j] = j;
//         }
//
//         for (int i = 1; i <= left.length(); i++) {
//             current[0] = i;
//             int rowMinimum = current[0];
//
//             for (int j = 1; j <= right.length(); j++) {
//                 int cost = left.charAt(i - 1) == right.charAt(j - 1)
//                         ? 0
//                         : 1;
//
//                 current[j] = Math.min(
//                         Math.min(current[j - 1] + 1, previous[j] + 1),
//                         previous[j - 1] + cost
//                 );
//
//                 rowMinimum = Math.min(rowMinimum, current[j]);
//             }
//
//             if (rowMinimum > maxDistance) {
//                 return maxDistance + 1;
//             }
//
//             int[] swap = previous;
//             previous = current;
//             current = swap;
//         }
//
//         return previous[right.length()];
//     }
//
//     private record Suggestion(String word, int distance) {
//     }
}

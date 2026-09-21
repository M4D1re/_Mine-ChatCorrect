package com.kaiki.minechatcorrect.spell;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;

class SpellCheckerTest {

    @Test
    void reusesResultsForUnchangedInput() {
        SpellChecker checker = new SpellChecker(Set.of("hello", "world"));

        List<MisspelledWord> errors = checker.findMisspellings("helo world");
        assertFalse(errors.isEmpty());
        assertSame(errors, checker.findMisspellings("helo world"));

        List<String> suggestions = checker.suggestionsFor("helo");
        assertFalse(suggestions.isEmpty());
        assertSame(suggestions, checker.suggestionsFor("helo"));
        assertSame(suggestions, checker.suggestionsFor("HELO"));

        assertTrue(checker.findMisspellings("hello world").isEmpty());
    }

    @Test
    void invalidatesCachedResultsWhenDictionaryChanges(@TempDir Path configDir) {
        SpellChecker checker = new SpellChecker(configDir);
        String word = "zzcacheprobe";

        assertFalse(checker.findMisspellings(word).isEmpty());
        assertFalse(checker.suggestionsFor(word).contains(word));

        checker.addWord(word);

        assertTrue(checker.findMisspellings(word).isEmpty());
        assertTrue(checker.suggestionsFor(word).contains(word));
    }

    @Test
    void findsRussianMisspellings() {
        SpellChecker checker = new SpellChecker(Set.of("привет", "мир"));

        List<MisspelledWord> misspellings =
                checker.findMisspellings("превет мир");

        assertEquals(
                List.of(new MisspelledWord("превет", 0, 6)),
                misspellings
        );
        assertEquals("привет", checker.bestSuggestionFor("превет"));
    }

    @Test
    void recognizesRussianCaseAndYo() {
        SpellChecker checker = new SpellChecker(Set.of("привет", "ёжик"));

        assertTrue(checker.findMisspellings("ПРИВЕТ Ёжик").isEmpty());

        assertEquals(
                List.of(new MisspelledWord("ёжк", 0, 3)),
                checker.findMisspellings("ёжк")
        );
        assertEquals("ёжик", checker.bestSuggestionFor("ёжк"));
    }

    @Test
    void checksMixedRussianAndEnglishText() {
        SpellChecker checker =
                new SpellChecker(Set.of("привет", "мир", "hello", "world"));

        assertTrue(checker.findMisspellings("Привет hello мир world").isEmpty());

        assertEquals(
                List.of(
                        new MisspelledWord("превет", 0, 6),
                        new MisspelledWord("wurld", 7, 12)
                ),
                checker.findMisspellings("превет wurld")
        );

        assertTrue(checker.findMisspellings("/msg Alex превет").isEmpty());
    }

    @Test
    void findsMisspellingsWhileIgnoringCommandsAndUrls() {
        SpellChecker checker = new SpellChecker(Set.of("hello", "world", "minecraft", "server", "visit"));

        assertTrue(checker.findMisspellings("/msg hello wurld").isEmpty());
        assertTrue(checker.findMisspellings("visit example.com hello").isEmpty());

        List<MisspelledWord> misspellings = checker.findMisspellings("hello wurld server");
        assertEquals(1, misspellings.size());
        assertEquals("wurld", misspellings.getFirst().word());
    }

    @Test
    void returnsDeterministicCorrectionSuggestions() {
        SpellChecker checker = new SpellChecker(Set.of("world", "word", "would", "wild", "hello"));

        List<String> suggestions = checker.suggestionsFor("wurld");

        assertFalse(suggestions.isEmpty());
        assertEquals("world", suggestions.getFirst());
        assertTrue(suggestions.contains("would"));
    }

    @Test
    void acceptedCustomWordsSuppressFalsePositivesInMemory() {
        SpellChecker checker = new SpellChecker(Set.of("kaiki", "minecraft", "chatcorrect", "uses"));

        assertTrue(checker.findMisspellings("Kaiki uses ChatCorrect").isEmpty());
        assertEquals(List.of("unknownword"), checker.findMisspellings("unknownword").stream().map(MisspelledWord::word).toList());
    }

    @Test
    void loadsBundledRussianDictionary(@TempDir Path configDir) {
        SpellChecker checker = new SpellChecker(configDir);

        assertTrue(
                checker.findMisspellings("Привет мир hello world Ёжик").isEmpty()
        );

        assertEquals(
                List.of(new MisspelledWord("превет", 0, 6)),
                checker.findMisspellings("превет мир")
        );

        assertTrue(checker.suggestionsFor("превет").contains("привет"));
    }

    @Test
    void keepsBundledRussianDictionaryAfterReload(@TempDir Path configDir) {
        SpellChecker checker = new SpellChecker(configDir);

        checker.reloadDictionaries();

        assertTrue(checker.findMisspellings("Привет мир Ёжик").isEmpty());
        assertTrue(checker.suggestionsFor("превет").contains("привет"));
    }

    @Test
    void recognizesRussianWordForms(@TempDir Path configDir) {
        SpellChecker checker = new SpellChecker(configDir);

        List<MisspelledWord> errors = checker.findMisspellings(
                "дом дома домами играть играет играли"
        );

        assertTrue(errors.isEmpty(), () -> "Не распознаны словоформы: " + errors);
    }

}

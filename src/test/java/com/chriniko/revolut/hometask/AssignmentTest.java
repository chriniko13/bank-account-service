package com.chriniko.revolut.hometask;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class AssignmentTest {

    @Test
    public void method_works_as_expected() {

        // given
        Map<Character, Long> expected = new LinkedHashMap<>();
        expected.put('a', 1L);
        expected.put('p', 2L);
        expected.put('l', 1L);
        expected.put('e', 1L);
        expected.put(' ', 1L);
        expected.put('!', 1L);


        String input = "apple !";
        Stream<Character> emitter = input.chars().mapToObj(c -> (char) c);


        // when
        Map<Character, Long> actual = Assignment.characterOccurrences(emitter);


        // then
        assertEquals(6, actual.size());
        assertEquals(expected, actual);

    }

    @Test
    public void method_works_as_expected_case2() {

        // given
        String input = "";
        Stream<Character> emitter = input.chars().mapToObj(c -> (char) c);


        // when
        Map<Character, Long> actual = Assignment.characterOccurrences(emitter);


        // then
        assertEquals(0, actual.size());
    }

    @Test(expected = IllegalArgumentException.class) // then
    public void method_works_as_expected_case3() {

        // when
        Assignment.characterOccurrences(null);
    }

}
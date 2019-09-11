package com.chriniko.revolut.hometask;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Assignment {


    public static Map<Character, Long> characterOccurrences(Stream<Character> input) {
        if (input == null) {
            throw new IllegalArgumentException("input should be provided");
        }
        return input.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    // ------- possible alternatives instead of using Stream<Character> as an input ----------

    class CharLazyList {
        Source head;
        Supplier<CharLazyList> tail;
    }

    public static class TextHolder {
        List<Source> inputs;
    }

    interface Source {
        String fetchInfo(int batchSize);
    }
}

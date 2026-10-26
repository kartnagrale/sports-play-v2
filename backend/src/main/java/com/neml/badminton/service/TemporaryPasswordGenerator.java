package com.neml.badminton.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TemporaryPasswordGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String symbols = "@#$%";
        String all = upper + lower + digits + symbols;
        List<Character> chars = new ArrayList<>();
        chars.add(pick(upper)); chars.add(pick(lower)); chars.add(pick(digits)); chars.add(pick(symbols));
        while (chars.size() < 14) chars.add(pick(all));
        Collections.shuffle(chars, random);
        StringBuilder value = new StringBuilder(chars.size());
        chars.forEach(value::append);
        return value.toString();
    }

    private char pick(String source) { return source.charAt(random.nextInt(source.length())); }
}

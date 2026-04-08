package com.awesomeapp.metric.java;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Validator421 {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private final List<String> errors = new ArrayList<>();

    public boolean validateNotNull(Object value, String fieldName) {
        if (value == null) { errors.add(fieldName + " must not be null"); return false; }
        return true;
    }

    public boolean validateStringLength(String value, String fieldName, int min, int max) {
        if (value == null || value.length() < min || value.length() > max) {
            errors.add(fieldName + " must be between " + min + " and " + max + " characters");
            return false;
        }
        return true;
    }

    public boolean validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Invalid email format"); return false;
        }
        return true;
    }

    public boolean validateRange(int value, String fieldName, int min, int max) {
        if (value < min || value > max) {
            errors.add(fieldName + " must be between " + min + " and " + max);
            return false;
        }
        return true;
    }

    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public void clearErrors() { errors.clear(); }
}

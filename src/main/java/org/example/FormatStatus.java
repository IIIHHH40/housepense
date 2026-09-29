package org.example;

public record FormatStatus(boolean isValid,String message) {

    public static FormatStatus valid() {
        return new FormatStatus(true, null);
    }

    public static FormatStatus invalid(String message) {
        return new FormatStatus(false, message);
    }

}

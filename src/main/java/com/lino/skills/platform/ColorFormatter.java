package com.lino.skills.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorFormatter {

    private static final Pattern HEX_TOKEN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BARE_HEX = Pattern.compile("(?<!&)#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final DecimalFormat DEC_FORMAT = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat INT_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private ColorFormatter() {
    }

    public static String translateLegacy(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_TOKEN.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder code = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                code.append('&').append(c);
            }
            matcher.appendReplacement(sb, code.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static Component render(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String normalized = normalizeHex(raw);
        return SERIALIZER.deserialize(normalized)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private static String normalizeHex(String text) {
        Matcher matcher = BARE_HEX.matcher(text);
        if (matcher.find()) {
            return matcher.replaceAll("&#$1");
        }
        return text;
    }

    public static String formatDecimal(double value) {
        return DEC_FORMAT.format(value);
    }

    public static String formatWhole(long value) {
        return INT_FORMAT.format(value);
    }

    public static String toRomanNumeral(int number) {
        if (number <= 0) {
            return "0";
        }
        StringBuilder result = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (remaining >= ROMAN_VALUES[i]) {
                remaining -= ROMAN_VALUES[i];
                result.append(ROMAN_SYMBOLS[i]);
            }
        }
        return result.toString();
    }

    public static String buildMeter(long current, long target, int segments, String activeColor, String emptyColor, String glyph) {
        double ratio = target > 0 ? Math.min(1.0, Math.max(0.0, (double) current / target)) : 1.0;
        int filled = (int) Math.round(ratio * segments);
        StringBuilder bar = new StringBuilder(activeColor);
        for (int i = 0; i < segments; i++) {
            if (i == filled) {
                bar.append(emptyColor);
            }
            bar.append(glyph);
        }
        return bar.toString();
    }

    public static List<String> wrapText(String input, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (input == null) {
            return lines;
        }
        String stripped = ChatColor.stripColor(translateLegacy(input));
        if (stripped == null || stripped.length() <= maxChars || maxChars <= 0) {
            lines.add(input);
            return lines;
        }

        String leadingSpaces = "";
        int idx = 0;
        while (idx < input.length() && input.charAt(idx) == ' ') {
            leadingSpaces += " ";
            idx++;
        }

        String[] words = input.trim().split(" ");
        StringBuilder currentLine = new StringBuilder(leadingSpaces);
        int visibleCount = 0;
        String activeColor = "&7";

        for (String word : words) {
            String cleanWord = ChatColor.stripColor(translateLegacy(word));
            int wordLen = cleanWord != null ? cleanWord.length() : word.length();
            if (visibleCount + wordLen > maxChars && visibleCount > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(leadingSpaces).append(activeColor).append(word);
                visibleCount = wordLen;
            } else {
                if (visibleCount > 0) {
                    currentLine.append(" ");
                    visibleCount++;
                }
                currentLine.append(word);
                visibleCount += wordLen;
            }
            String extracted = extractLastColors(word);
            if (!extracted.isEmpty()) {
                activeColor = extracted;
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private static String extractLastColors(String text) {
        Matcher hexMatcher = HEX_TOKEN.matcher(text);
        String last = "";
        while (hexMatcher.find()) {
            last = hexMatcher.group(0);
        }
        String legacy = ChatColor.getLastColors(text);
        return !legacy.isEmpty() ? legacy : last;
    }
}

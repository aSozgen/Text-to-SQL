package com.texttosql.backend.util;

import java.util.regex.Pattern;

public class SqlFormatter {

    private static final Pattern SELECT_PATTERN = Pattern.compile("\\bSELECT\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_PATTERN = Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_PATTERN = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile("\\b(INNER|LEFT|RIGHT|FULL|CROSS)\\s+JOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_PATTERN = Pattern.compile("\\bON\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile("\\bGROUP\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HAVING_PATTERN = Pattern.compile("\\bHAVING\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("\\bORDER\\s+BY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\bLIMIT\\b", Pattern.CASE_INSENSITIVE);

    public static String format(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        String formatted = sql.trim();

        // Add line breaks before major clauses
        formatted = SELECT_PATTERN.matcher(formatted).replaceAll("\nSELECT");
        formatted = FROM_PATTERN.matcher(formatted).replaceAll("\nFROM");
        formatted = WHERE_PATTERN.matcher(formatted).replaceAll("\nWHERE");
        formatted = JOIN_PATTERN.matcher(formatted).replaceAll("\n$1 JOIN");
        formatted = ON_PATTERN.matcher(formatted).replaceAll("\n  ON");
        formatted = GROUP_BY_PATTERN.matcher(formatted).replaceAll("\nGROUP BY");
        formatted = HAVING_PATTERN.matcher(formatted).replaceAll("\nHAVING");
        formatted = ORDER_BY_PATTERN.matcher(formatted).replaceAll("\nORDER BY");
        formatted = LIMIT_PATTERN.matcher(formatted).replaceAll("\nLIMIT");

        // Clean up multiple spaces
        formatted = formatted.replaceAll("  +", " ");

        // Clean up multiple newlines
        formatted = formatted.replaceAll("\n\n+", "\n");

        return formatted.trim();
    }

    public static boolean isValidSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String upperSql = sql.trim().toUpperCase();

        // Basic validation - should start with a SQL keyword
        return upperSql.startsWith("SELECT") ||
                upperSql.startsWith("INSERT") ||
                upperSql.startsWith("UPDATE") ||
                upperSql.startsWith("DELETE") ||
                upperSql.startsWith("CREATE") ||
                upperSql.startsWith("ALTER") ||
                upperSql.startsWith("DROP") ||
                upperSql.startsWith("WITH");
    }

    public static String sanitize(String sql) {
        if (sql == null) {
            return null;
        }

        // Remove dangerous keywords
        String sanitized = sql;
        sanitized = sanitized.replaceAll("(?i)\\bDROP\\s+DATABASE\\b", "");
        sanitized = sanitized.replaceAll("(?i)\\bDROP\\s+TABLE\\b", "");
        sanitized = sanitized.replaceAll("(?i)\\bTRUNCATE\\b", "");
        sanitized = sanitized.replaceAll("(?i)\\bDELETE\\s+FROM\\b", "");

        return sanitized.trim();
    }
}

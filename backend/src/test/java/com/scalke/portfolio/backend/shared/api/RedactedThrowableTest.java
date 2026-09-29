package com.scalke.portfolio.backend.shared.api;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D-DB : la copie journalisée garde les types, les piles et les causes, jamais les messages.
 */
class RedactedThrowableTest {

    @Test
    void keeps_types_stack_traces_and_causes_without_messages() {
        SQLException sql = new SQLException("FATAL: password authentication failed for user portfolio / s3cr3t", "28P01");
        IllegalStateException original = new IllegalStateException("contact alice@example.test : bonjour", sql);
        original.addSuppressed(new IllegalArgumentException("jeton abc123"));

        String printed = print(RedactedThrowable.of(original));

        assertThat(printed)
            .contains("java.lang.IllegalStateException (message masqué)")
            .contains("java.sql.SQLException [SQLState 28P01] (message masqué)")
            .contains("java.lang.IllegalArgumentException (message masqué)")
            .contains(RedactedThrowableTest.class.getName())
            .doesNotContain("s3cr3t", "alice@example.test", "bonjour", "abc123", "password authentication");
        assertThat(RedactedThrowable.of(original).getStackTrace()).isEqualTo(original.getStackTrace());
    }

    private static String print(Throwable throwable) {
        StringWriter out = new StringWriter();
        throwable.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}

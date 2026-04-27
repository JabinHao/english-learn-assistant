package com.ailearn.service.learning;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParagraphSplitServiceTest {

    @Test
    void split_shouldReturnTrimmedParagraphs() {
        ParagraphSplitService service = new ParagraphSplitService();

        assertThat(service.split("""
                First paragraph.

                  Second paragraph with extra space.


                Third paragraph.
                """)).containsExactly(
                "First paragraph.",
                "Second paragraph with extra space.",
                "Third paragraph."
        );
    }
}

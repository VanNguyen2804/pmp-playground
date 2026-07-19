package com.example.pmp.question;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeQuestionSelectionServiceTest {

    @Test
    void prioritizesCurrentMistakesAndUnansweredQuestions() {
        double unanswered = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(0, 0, 0, null));
        double currentMistake = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(4, 1, 3, false));
        double learning = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(4, 3, 1, true));

        assertThat(currentMistake).isGreaterThan(unanswered);
        assertThat(unanswered).isGreaterThan(learning);
    }

    @Test
    void lowersFrequencyAfterFiveCorrectAnswers() {
        double mastered = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(7, 5, 2, true));
        double notMastered = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(4, 4, 0, true));
        double unanswered = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(0, 0, 0, null));

        assertThat(mastered).isLessThan(notMastered);
        assertThat(mastered).isLessThan(unanswered);
    }

    @Test
    void aNewWrongAnswerRestoresHighPriorityEvenAfterPriorCorrectAnswers() {
        double masteredButWrongNow = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(9, 6, 3, false));
        double masteredAndCorrectNow = PracticeQuestionSelectionService.weightFor(
                new PracticeQuestionSelectionService.PracticeAttemptStats(9, 6, 3, true));

        assertThat(masteredButWrongNow).isGreaterThan(masteredAndCorrectNow);
    }
}

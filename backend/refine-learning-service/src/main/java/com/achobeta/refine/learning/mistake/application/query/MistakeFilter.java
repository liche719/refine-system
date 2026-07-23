package com.achobeta.refine.learning.mistake.application.query;

import com.achobeta.refine.learning.mistake.domain.MistakeReason;

import java.time.LocalDate;
import java.util.List;

public record MistakeFilter(String keyword, List<String> subjects, List<MistakeReason> reasons,
                            LocalDate startDate, LocalDate endDate) {
    public MistakeFilter {
        subjects = subjects == null ? List.of() : List.copyOf(subjects);
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static MistakeFilter unfiltered() {
        return new MistakeFilter(null, List.of(), List.of(), null, null);
    }
}

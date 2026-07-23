package com.achobeta.refine.learning.mistake.application.query;

import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;

import java.util.List;

public record MistakePage(List<MistakeQuestion> content, Pageable pageable, boolean last, long totalPages,
                          long totalElements, int size, int number, Sort sort, boolean first,
                          int numberOfElements, boolean empty) {
    public MistakePage {
        content = List.copyOf(content);
    }

    public static MistakePage of(List<MistakeQuestion> content, long totalElements, long totalPages,
                                 int size, int number, boolean first, boolean last) {
        List<MistakeQuestion> safeContent = List.copyOf(content);
        Sort sort = Sort.defaultSort();
        Pageable pageable = new Pageable(number, size, sort, (long) number * size, true, false);
        return new MistakePage(safeContent, pageable, last, totalPages, totalElements, size, number, sort,
                first, safeContent.size(), safeContent.isEmpty());
    }

    public record Pageable(int pageNumber, int pageSize, Sort sort, long offset, boolean paged, boolean unpaged) { }

    public record Sort(boolean empty, boolean sorted, boolean unsorted) {
        private static Sort defaultSort() {
            return new Sort(true, false, true);
        }
    }
}

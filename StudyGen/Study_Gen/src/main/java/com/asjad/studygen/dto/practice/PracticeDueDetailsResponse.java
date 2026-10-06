package com.asjad.studygen.dto.practice;

import java.util.List;

public record PracticeDueDetailsResponse(
        int dueCount,
        int totalCards,
        int streakDays,
        List<RoadmapModuleBreakdown> breakdown,
        List<UpcomingDaySchedule> upcomingSchedule,
        List<PracticeCardDTO> dueCards
) {}

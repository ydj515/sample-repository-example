package com.example.jpajavaexample;

import com.example.jpajavaexample.domain.performance.model.Performance;
import com.example.jpajavaexample.domain.performance.repository.PerformanceRepository;
import com.example.jpajavaexample.domain.user.model.User;
import com.example.jpajavaexample.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitDataInitializer implements ApplicationRunner {

    private static final int TARGET_SAMPLE_USER_COUNT = 100;
    private static final int TARGET_SAMPLE_PERFORMANCE_COUNT = 100;
    private static final List<String> PERFORMANCE_CATEGORIES = List.of(
        "MUSICAL",
        "CONCERT",
        "PLAY",
        "CLASSIC",
        "EXHIBITION"
    );

    private final UserRepository userRepository;
    private final PerformanceRepository performanceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initUsers();
        initPerformances();
    }

    private void initUsers() {
        long currentCount = userRepository.findAll(PageRequest.of(0, 1))
            .getTotalElements();

        if (currentCount >= TARGET_SAMPLE_USER_COUNT) {
            return;
        }

        int toCreate = (int) (TARGET_SAMPLE_USER_COUNT - currentCount);
        int index = 1;
        int created = 0;

        while (created < toCreate) {
            String email = sampleEmail(index);

            if (!userRepository.existsByEmail(email)) {
                User user = User.create(sampleName(index), email, samplePassword(index));
                userRepository.save(user);
                created++;
            }

            index++;
        }
    }

    private void initPerformances() {
        long currentCount = performanceRepository.count();

        if (currentCount >= TARGET_SAMPLE_PERFORMANCE_COUNT) {
            return;
        }

        long toCreate = TARGET_SAMPLE_PERFORMANCE_COUNT - currentCount;
        for (long offset = 0; offset < toCreate; offset++) {
            long sequence = currentCount + offset + 1;
            LocalDate startDate = samplePerformanceStartDate(sequence);
            LocalDate endDate = samplePerformanceEndDate(startDate);

            Performance performance = Performance.create(
                samplePerformanceTitle(sequence),
                samplePerformanceCategory(sequence),
                startDate,
                endDate
            );

            performanceRepository.save(performance);
        }
    }

    private String sampleName(int index) {
        return "Sample User " + index;
    }

    private String sampleEmail(int index) {
        return "sample-user-%03d@example.com".formatted(index);
    }

    private String samplePassword(int index) {
        return "password-%03d".formatted(index);
    }

    private String samplePerformanceTitle(long index) {
        return "Sample Performance %03d".formatted(index);
    }

    private String samplePerformanceCategory(long index) {
        int categoryIndex = (int) ((index - 1) % PERFORMANCE_CATEGORIES.size());
        return PERFORMANCE_CATEGORIES.get(categoryIndex);
    }

    private LocalDate samplePerformanceStartDate(long index) {
        return LocalDate.of(2024, 1, 1).plusDays(index - 1);
    }

    private LocalDate samplePerformanceEndDate(LocalDate startDate) {
        return startDate.plusDays(6);
    }
}

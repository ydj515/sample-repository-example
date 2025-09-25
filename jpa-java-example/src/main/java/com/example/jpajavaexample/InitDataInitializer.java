package com.example.jpajavaexample;

import com.example.jpajavaexample.domain.user.model.User;
import com.example.jpajavaexample.domain.user.repository.UserRepository;
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

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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

    private String sampleName(int index) {
        return "Sample User " + index;
    }

    private String sampleEmail(int index) {
        return "sample-user-%03d@example.com".formatted(index);
    }

    private String samplePassword(int index) {
        return "password-%03d".formatted(index);
    }
}

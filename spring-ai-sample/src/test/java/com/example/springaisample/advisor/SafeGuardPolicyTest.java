package com.example.springaisample.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class SafeGuardPolicyTest {

    @Test
    void fromResourceLoadsSensitiveWordsIgnoringCommentsAndBlankLines() {
        ByteArrayResource resource = new ByteArrayResource("""
                # 민감 단어 목록

                스미싱
                무기
                  비밀번호
                """.getBytes(StandardCharsets.UTF_8));

        SafeGuardPolicy policy = SafeGuardPolicy.fromResource(resource);

        assertThat(policy.sensitiveWords()).containsExactly("스미싱", "무기", "비밀번호");
        assertThat(policy.blockedMessage()).isEqualTo(SafeGuardPolicy.DEFAULT_BLOCKED_MESSAGE);
    }
}

package com.example.uristrangeexample;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class UriTest {

    @Test
    public void removedUrlFailTest() {
        String url = "toss://send_money?min_version=2.22.1&data=";
        URI uri = getUri(url);
        String removedUrl = getRemovedUrl(uri);

        assertThat(removedUrl).isEqualTo(url);
    }

    @Test
    public void removedUrlSuccessTest() {
        String url = "toss://sendmoney?min_version=2.22.1&data=";
        URI uri = getUri(url);
        String removedUrl = getRemovedUrl(uri);

        assertThat(removedUrl).isEqualTo(url);
    }

    @Test
    public void removedUrlSuccessUseGetRemovedUrlStringTest() {
        String url = "toss://send_money?min_version=2.22.1&data=";
        String removedUrl = getRemovedUrlString(url);

        assertThat(removedUrl).isEqualTo(url);
    }

    public URI getUri(String url) {
        return URI.create(url);
    }

    public String getRemovedUrl(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .toUriString();
    }

    public String getRemovedUrlString(String uri) {
        return UriComponentsBuilder.fromUriString(uri)
                .build()
                .toUriString();
    }
}

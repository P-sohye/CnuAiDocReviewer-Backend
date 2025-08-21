// com.cnu.docserver.common.RestClientConfig.java
package com.cnu.docserver.common;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .additionalMessageConverters(new ByteArrayHttpMessageConverter())
                .build();
    }

    /** OCR 전용 RestTemplate (타임아웃 여유) */
    @Bean(name = "ocrRestTemplate")
    public RestTemplate ocrRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120))
                .additionalMessageConverters(new ByteArrayHttpMessageConverter())
                .build();
    }
}

package com.example.dodo.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpConfig {
    @Bean
    public RestTemplate restTemplate() {
        RequestConfig rc = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(180))
                .build();
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(rc)
                .build();
        var factory = new HttpComponentsClientHttpRequestFactory(client);
        return new RestTemplate(factory);
    }
}


package com.tradenotes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@MapperScan("com.tradenotes.mapper")
public class TradeNotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeNotesApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate rt = builder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(10))
                .build();
        // 默认 StringHttpMessageConverter 用 ISO-8859-1，会导致中文乱码
        rt.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        rt.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return rt;
    }
}

package com.mvasilakos.filestorage.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cors configuration properties.
 */
@Configuration
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {

  private List<String> allowedOrigins;
  private List<String> allowedMethods;
  private String allowedHeaders;
  private Boolean allowCredentials;
  private Long maxAge;
}
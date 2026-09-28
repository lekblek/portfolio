package com.scalke.portfolio.backend.media.infrastructure.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MediaStorageProperties.class)
class MediaStorageConfiguration {
}

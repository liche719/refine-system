package com.achobeta.refine.identity.account.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MailProfileConfigurationTest {
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void defaultProfileUsesAuthenticatedQqSmtpOverStartTls() throws IOException {
        List<PropertySource<?>> properties = load("application.yml");

        assertThat(value(properties, "spring.mail.host")).isEqualTo("smtp.qq.com");
        assertThat(value(properties, "spring.mail.port")).isEqualTo(587);
        assertThat(value(properties, "spring.mail.username").toString()).startsWith("${MAIL_USERNAME");
        assertThat(value(properties, "spring.mail.password").toString()).startsWith("${MAIL_PASSWORD");
        assertThat(value(properties, "spring.mail.properties.mail.smtp.auth")).isEqualTo(true);
        assertThat(value(properties, "spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo(true);
        assertThat(value(properties, "spring.mail.properties.mail.smtp.starttls.required")).isEqualTo(true);
        assertThat(value(properties, "spring.mail.properties.mail.smtp.ssl.enable")).isNull();
        assertThat(value(properties, "refine.mail.from")).isEqualTo("${spring.mail.username}");
    }

    @Test
    void productionProfileTestsTheSmtpConnectionAtStartup() throws IOException {
        List<PropertySource<?>> properties = load("application-prod.yml");

        assertThat(value(properties, "spring.mail.test-connection")).isEqualTo(true);
    }

    @Test
    void dockerProfileDoesNotOverrideTheExternalSmtpConfiguration() throws IOException {
        List<PropertySource<?>> properties = load("application-docker.yml");

        assertThat(value(properties, "spring.mail.host")).isNull();
        assertThat(value(properties, "spring.mail.port")).isNull();
    }

    private List<PropertySource<?>> load(String resource) throws IOException {
        return loader.load(resource, new ClassPathResource(resource));
    }

    private Object value(List<PropertySource<?>> properties, String key) {
        return properties.stream()
                .map(propertySource -> propertySource.getProperty(key))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }
}

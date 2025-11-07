package com.example.hotelreservationsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration

public class RedisConfig {
    /**
     * Configuration class for Redis integration.
     * <p>
     * This class defines a {@link RedisTemplate} bean with custom serializers for keys and values.
     * By setting {@link StringRedisSerializer} for keys, values, hash keys, and hash values,
     * it ensures that all data stored in Redis is serialized as plain strings.
     * This approach improves compatibility, readability, and interoperability with other systems
     * that may access Redis directly, and avoids issues with default Java serialization.
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        var template = new RedisTemplate<String, String>();
        template.setConnectionFactory(connectionFactory);

        var stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}

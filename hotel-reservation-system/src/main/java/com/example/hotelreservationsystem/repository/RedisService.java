package com.example.hotelreservationsystem.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

//@Autowired
//private RedisService redisService;
//
//redisService.save("test", "value");

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
package com.example.librarymanagementsystem.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Bounded in-process caches for frequently read catalogue data.
 * Search pages have a short TTL because stock availability changes during circulation.
 */
@Configuration
@EnableCaching
public class CatalogueCacheConfig {
    public static final String SEARCH_CACHE = "catalogueSearch";
    public static final String CATEGORY_CACHE = "catalogueCategories";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache search = new CaffeineCache(SEARCH_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(Duration.ofMinutes(2))
                        .recordStats()
                        .build());
        CaffeineCache categories = new CaffeineCache(CATEGORY_CACHE,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(search, categories));
        return manager;
    }
}

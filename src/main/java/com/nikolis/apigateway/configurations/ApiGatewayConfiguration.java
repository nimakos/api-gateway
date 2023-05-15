package com.nikolis.apigateway.configurations;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import reactor.core.publisher.Mono;

import java.awt.print.Book;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class ApiGatewayConfiguration {
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("route_1", p -> p
                        .path("/get")
                        //.filters(f -> f.addRequestHeader("MyHeader", "MyURI").addRequestParameter("Param", "MyValue"))
                        .uri("http://httpbin.org:80"))
                .route("route_2", p -> p
                        .path("/recommendation-service/**")
                        .filters(f -> f.requestRateLimiter(r -> r
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(proxyClientAddressResolver())
                                        .setKeyResolver(keyResolver())
                                        .setKeyResolver(simpleClientAddressResolver())
                                        .setDenyEmptyKey(false)
                        ))
                        .uri("lb://recommendation-service")
                )
                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver keyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    @Bean
    public KeyResolver simpleClientAddressResolver() {
        return exchange -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(InetAddress::getHostAddress)
                .map(Mono::just)
                .orElse(Mono.empty());
    }

    /**
     * In case we use Proxy server for our clients
     * @return The Key
     */
    @Bean
    @Primary
    public KeyResolver proxyClientAddressResolver() {
        return exchange -> Mono.just(XForwardedRemoteAddressResolver.maxTrustedIndex(1).resolve(exchange).getAddress().getHostAddress());
    }


    @Bean
    public RedisTemplate<Long, Book> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Long, Book> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}

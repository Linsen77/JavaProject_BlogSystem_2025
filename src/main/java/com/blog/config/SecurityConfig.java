package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 认证接口
                        .requestMatchers("/api/auth/**").permitAll()

                        // 用户接口
                        .requestMatchers("/api/users/**").permitAll()

                        // 文章接口（全部放行）
                        .requestMatchers("/api/articles/**").permitAll()

                        .requestMatchers(HttpMethod.DELETE, "/api/articles/*/bookmark").permitAll()

                        // 关注接口
                        .requestMatchers(HttpMethod.DELETE, "/api/follow/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/follow/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follow/**").permitAll()
                        .requestMatchers("/api/follow/**").permitAll()


                        // 上传接口
                        .requestMatchers("/api/upload").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // 静态资源
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/avatar/**").permitAll()

                        // WebSocket
                        .requestMatchers("/ws/**", "/notifications/**").permitAll()

                        .requestMatchers("/api/notifications/**").permitAll()

                        // 其他全部需要登录
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}

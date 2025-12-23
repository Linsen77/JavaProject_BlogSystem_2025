package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
                        // 允许所有认证相关接口
                        .requestMatchers("/api/auth/**").permitAll()

                        // 允许文章接口
                        .requestMatchers("/api/articles/**").permitAll()

                        .requestMatchers("/api/articles/*/likes").permitAll()

                        .requestMatchers("/api/articles/*/like").permitAll()

                        .requestMatchers("/api/articles/*/comments").permitAll()

                        .requestMatchers("/api/articles/*/bookmarks").permitAll()

                        // 允许 WebSocket
                        .requestMatchers("/ws/**","/notifications/**").permitAll()

                        // 允许静态资源
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**").permitAll()

                        .requestMatchers("/api/users/**").permitAll()

                        .requestMatchers("/avatar/**").permitAll()

                        // 其他接口需要登录
                        .anyRequest().authenticated()
                )
                // 禁用 HTTP Basic（否则会干扰前端）
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/notifications/**");
    }

}
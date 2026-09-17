package cn.edu.gdc.zy.ai_career_helper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 完全禁用 CSRF（跨站请求伪造）保护，方便浏览器和前端直接测试
                .csrf(csrf -> csrf.disable())
                // 2. 彻底放开所有接口请求，Spring Security 不再拦截任何 URL
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 3. 关键：禁用 Spring Security 默认的登录页和表单拦截机制
                //    因为你的登录逻辑是自己写在 Service 里的，不需要它来接管
                .formLogin(form -> form.disable());

        return http.build();
    }
}
package com.qlytro.config;

import com.qlytro.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // Login page & error pages
                .requestMatchers("/login", "/error", "/error/**").permitAll()
                // Landlord area
                .requestMatchers("/landlord/**").hasRole("LANDLORD")
                // Tenant area
                .requestMatchers("/tenant/**").hasRole("TENANT")
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }

    /**
     * Sau khi dang nhap thanh cong, chuyen huong theo vai tro:
     * CHU_TRO -> /landlord/dashboard
     * KHACH_THUE -> /tenant/dashboard
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication.getPrincipal() instanceof com.qlytro.security.CustomUserDetails userDetails) {
                if (userDetails.isMustChangePassword()) {
                    response.sendRedirect("/doi-mat-khau?required=true");
                    return;
                }
            }
            boolean isLandlord = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_LANDLORD"));
            if (isLandlord) {
                response.sendRedirect("/landlord/dashboard");
            } else {
                response.sendRedirect("/tenant/dashboard");
            }
        };
    }
}

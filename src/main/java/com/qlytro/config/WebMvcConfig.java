package com.qlytro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final com.qlytro.security.ForceChangePasswordInterceptor forceChangePasswordInterceptor;

    public WebMvcConfig(com.qlytro.security.ForceChangePasswordInterceptor forceChangePasswordInterceptor) {
        this.forceChangePasswordInterceptor = forceChangePasswordInterceptor;
    }

    /**
     * Phuc vu file upload (anh chi so, anh chuyen khoan) tu thu muc local.
     * URL /uploads/** -> thu muc app.upload.dir tren filesystem.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + "/");
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(forceChangePasswordInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/logout", "/error", "/error/**", "/css/**", "/js/**", "/images/**");
    }
}

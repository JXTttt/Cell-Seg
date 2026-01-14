package com.urine.cell_seg_sys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 意思是：当浏览器访问 /images/xxxxx 时，去 E:/code/upload_files/ 找文件
        // 注意：Windows 路径最后一定要加双斜杠 \\
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:E:\\code\\upload_files\\");
    }
}
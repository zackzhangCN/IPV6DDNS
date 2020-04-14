package cn.zack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务支持
 */
@EnableScheduling
@SpringBootApplication
public class IPV6DDNSApplication {
    public static void main(String[] args) {
        SpringApplication.run(IPV6DDNSApplication.class, args);
    }
}
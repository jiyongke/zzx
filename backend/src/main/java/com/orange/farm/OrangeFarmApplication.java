package com.orange.farm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 橙心助农农产品溯源电商平台 - 启动类
 */
@SpringBootApplication
@MapperScan("com.orange.farm.mapper")
public class OrangeFarmApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrangeFarmApplication.class, args);
        System.out.println("========================================");
        System.out.println("橙心助农溯源电商平台后端启动成功，访问 http://localhost:8080");
        System.out.println("========================================");
    }
}

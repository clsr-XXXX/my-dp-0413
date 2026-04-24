package com.att;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.att.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class ATTApplication {

    public static void main(String[] args) {
        SpringApplication.run(ATTApplication.class, args);
    }

}

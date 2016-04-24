package com.gary.thriftext.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.locks.LockSupport;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 16-4-23 下午5:24
 */public class ClientSpring {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring-client.xml");
        context.start();
        LockSupport.park();
    }
}

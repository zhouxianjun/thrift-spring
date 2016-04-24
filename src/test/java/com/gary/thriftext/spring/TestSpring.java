package com.gary.thriftext.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.locks.LockSupport;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2016/4/22 14:35
 */
public class TestSpring {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        context.start();
        LockSupport.park();
    }
}

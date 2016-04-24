package com.gary.thriftext.spring.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2016/4/22 9:58
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ThriftReference {
    String name() default "";

    String version() default "1.0.0";
}

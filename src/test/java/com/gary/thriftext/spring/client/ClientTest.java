package com.gary.thriftext.spring.client;

import com.gary.thrift.Demo;
import com.gary.thriftext.spring.annotation.ThriftReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 16-4-23 下午5:29
 */
@Component
public class ClientTest implements InitializingBean {
    @ThriftReference
    private Demo.Iface demo;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(demo.say("xxxxxxxxxxxxxxxxxxxx"));
    }
}

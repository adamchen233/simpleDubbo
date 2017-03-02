# simpleDubbo

一个分布式系统中的服务框架中间件，功能上类似于简化版的dubbo，即为分布式的服务层提供远程调用功能，基于spring配置，通信路由部分使用了zookeeper作为通信过程的服务注册查找中心，项目涉及动态代理、反射、 NIO通信、多线程/线程池等技术的使用。



使用说明：导入项目之后，在客户端和服务端需要一样的接口，然后服务端要有实现类。
在服务端，bean配置：
```
    <bean id="providerBean" class="com.adamchen233.simpleDubbo.ProviderBean">
        <property name="interfaceName" value="{your own interfaceName}"/>
        <property name="version" value="1.0.0"/>
        <property name="target" ref="{the implClass id}"/>
        <property name="group" value="Test"/>
        <property name="registerAddress" value="{zk address and port}"/>
    </bean>
    <bean id="Impl" class="{the implClass}"></bean>

```
获取providerBean后调用listen(port);

在客户端方,spring配置bean:
```
    <bean id="consumerBean" class="com.adamchen233.simpleDubbo.ConsumerBean">
        <property name="interfaceName" value="{your own interfaceName}"/>
        <property name="version" value="1.0.0"/>
        <property name="group" value="Test"/>
        <property name="registerAddress" value="{zk address and port}"/>
    </bean>
```
获取bean对象之后调用getProxy()获取代理对象,进行远程调用。

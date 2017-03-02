/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adamchen233.simpleDubbo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.io.XMLWriter;

/**
 *
 * @author xiaohui
 */
public class ConsumerBean {

    private final static Logger logger = Logger.getLogger(ConsumerBean.class.getName());

    private String interfaceName;
    private String version;
    private String group;
    private String registerAddress;
    private String URI;

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public void setGroup(String group) {
        this.group = group;
    }
    public void setRegisterAddress(String registerAddress) {
        this.registerAddress = registerAddress;
    }
    
    public Object getProxy() {
        //获取可用服务地址列表,zookeeper
        Naming zkNaming = new Naming(registerAddress, "/" + interfaceName);
        List<String> serviceList = zkNaming.Readall();
        for (String service : serviceList) {
            System.out.println(service); //172.16.219.212:4567#com.mycompany.simpledubbodemo.DemoService
        }

        //确定要调用服务的目的机器,用的策略是随机随机
        Random random = new Random();
        int index = random.nextInt(serviceList.size());
        URI = serviceList.get(index);
        
        //生产动态代理对象
        ConsumerProxyHandler handler = new ConsumerProxyHandler();
        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), getIntefaces(), handler);
        return proxy;
    }

    private Class[] getIntefaces() {
        Class[] interfaces = new Class[1];
        try {
            interfaces[0] = Class.forName(interfaceName);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConsumerBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return interfaces;
    }

    class ConsumerProxyHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            //当动态代理对象被调用时

            ResponseObject responseObject = null;
            
            
            //从URI中获得host和port
            String hostName = URI.split(":")[0];
            int port = Integer.valueOf(URI.split(":")[1].split("#")[0]);
            SocketChannel socketChannel = null;
            try {
                socketChannel = SocketChannel.open();
                SocketAddress socketAddress = new InetSocketAddress(hostName, port);
                socketChannel.connect(socketAddress);
                RequestObject requestObject = new RequestObject(interfaceName, method.getName(), args);
                logger.log(Level.INFO, requestObject.toString());
                sendData(socketChannel, requestObject);

                responseObject = receiveData(socketChannel);
                logger.log(Level.INFO, responseObject.toString());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            } finally {
                try {
                    socketChannel.close();
                } catch (Exception ex) {
                }
            }
            return responseObject.getResultObject();
        }

        private void sendData(SocketChannel socketChannel, RequestObject requestObject) throws IOException {
            byte[] bytes = SerializableUtil.toBytes(requestObject);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            socketChannel.write(buffer);
            socketChannel.socket().shutdownOutput();
        }

        private ResponseObject receiveData(SocketChannel socketChannel) throws IOException {
            ResponseObject responseObject = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try {
                ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
                byte[] bytes;
                int count = 0;
                while ((count = socketChannel.read(buffer)) >= 0) {
                    buffer.flip();
                    bytes = new byte[count];
                    buffer.get(bytes);
                    baos.write(bytes);
                    buffer.clear();
                }
                bytes = baos.toByteArray();
                Object obj = SerializableUtil.toObject(bytes);
                responseObject = (ResponseObject) obj;
                socketChannel.socket().shutdownInput();
            } finally {
                try {
                    baos.close();
                } catch (Exception ex) {
                }
            }
            return responseObject;
        }

    }
}

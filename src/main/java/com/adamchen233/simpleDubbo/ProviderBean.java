/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adamchen233.simpleDubbo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author xiaohui
 */
public class ProviderBean {
    
    private final static Logger logger = Logger.getLogger(ProviderBean.class.getName());
    
    private String interfaceName;
    private String version;
    private String group;
    private Object target;
    private String registerAddress;

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public void setGroup(String group) {
        this.group = group;
    }
    public void setTarget(Object target) {
        this.target = target;
    }
    public void setRegisterAddress(String registerAddress) {
        this.registerAddress = registerAddress;
    }
    
    private Class[] objectArr2ClassArr(Object[] args) throws ClassNotFoundException{
        Class[] classes = new Class[args.length];
        for(int i = 0; i < args.length; i ++){
            classes[i] = args[i].getClass();
        }
        return classes;
    }
    
    
    public void listen(int port) throws UnknownHostException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException{
        //向zk里注册信息，完成服务的发布
        Naming zkNaming = new Naming(registerAddress,"/"+interfaceName);
        InetAddress addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();
        
        //serviceAddress注册服务的信息格式为：   host:port#con.xxxx.service
        String serviceAddress = ip + ":" + port + "#" +interfaceName;

        int registerResult = zkNaming.Registered(serviceAddress);
        if(registerResult == 1){
            //已被注册
            System.out.print("!!!!!!!!!!REGISTERED!!!!!!");
        }else if(registerResult == -1){
            //出错
            System.out.print("!!!!!!!!!!ERROR!!!");
        }else if(registerResult == 0){
            //成功
            System.out.print("!!!!!!!!!!!GOOD!!!!!!");
        }
        
        //注销zk服务信息
        //int cancelResult = zkNaming.Canceled(serviceAddress);

        
        //开始使用selector和channel进行监听
        Selector selector = null;
        ServerSocketChannel serverSocketChannel = null;

        try {
                selector = Selector.open();

                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);

                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(port));
                
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                while (selector.select() > 0) {
                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                        
                        while (it.hasNext()) {
                                SelectionKey readyKey = it.next();
                                it.remove();
                                
                                execute((ServerSocketChannel) readyKey.channel());
                        }
                }
        } catch (ClosedChannelException ex) {
                logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
        } finally {
                try {
                    selector.close();
                } catch(Exception ex) {}
                try {
                    serverSocketChannel.close();
                } catch(Exception ex) {}
        }

    }
    private void execute(ServerSocketChannel serverSocketChannel) throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
            SocketChannel socketChannel = null;
            try {
                    socketChannel = serverSocketChannel.accept();
                    RequestObject requestObject = receiveData(socketChannel);
                    logger.log(Level.INFO, requestObject.toString());

                    //在这里调用委托类
                    Object[] args = requestObject.getArgs();
                    Class[] argClassArr = objectArr2ClassArr(args);
                    Method method = null;  
                    
                    for(Method method4Class : Class.forName(interfaceName).getMethods()){
                        boolean flag = true;
                        if(method4Class.getName().equals(requestObject.getServiceMethod()) && method4Class.getParameterTypes().length == argClassArr.length){
                            for(int i = 0; i < argClassArr.length; i ++){
                                if( !(argClassArr[i].isAssignableFrom(method4Class.getParameterTypes()[i])
                                        || argClassArr[i].getName().equals("java.lang.Byte") && method4Class.getParameterTypes()[i].getName().equals("byte")
                                        || argClassArr[i].getName().equals("java.lang.Short") && method4Class.getParameterTypes()[i].getName().equals("short")
                                        || argClassArr[i].getName().equals("java.lang.Integer") && method4Class.getParameterTypes()[i].getName().equals("integer")
                                        || argClassArr[i].getName().equals("java.lang.Long") && method4Class.getParameterTypes()[i].getName().equals("long")
                                        || argClassArr[i].getName().equals("java.lang.Float") && method4Class.getParameterTypes()[i].getName().equals("float")
                                        || argClassArr[i].getName().equals("java.lang.Double") && method4Class.getParameterTypes()[i].getName().equals("double")
                                        || argClassArr[i].getName().equals("java.lang.Character") && method4Class.getParameterTypes()[i].getName().equals("character")
                                        || argClassArr[i].getName().equals("java.lang.Boolean") && method4Class.getParameterTypes()[i].getName().equals("coolean")
                                        )){
                                    flag = false;
                                }
                            }
                            if(flag){
                                method = method4Class;
                            }
                        }
                    }
                    
                    Object resultObject = method.invoke(target, args);
                    
                    ResponseObject responseObject = new ResponseObject(resultObject);
                    sendData(socketChannel, responseObject);
                    logger.log(Level.INFO, responseObject.toString());
            } finally {
                    try {
                            socketChannel.close();
                    } catch(Exception ex) {}
            }
    }

    private RequestObject receiveData(SocketChannel socketChannel) throws IOException {
            RequestObject requestObject = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            try {
                    byte[] bytes;
                    int size = 0;
                    while ((size = socketChannel.read(buffer)) >= 0) {
                            buffer.flip();
                            bytes = new byte[size];
                            buffer.get(bytes);
                            baos.write(bytes);
                            buffer.clear();
                    }
                    bytes = baos.toByteArray();
                    Object obj = SerializableUtil.toObject(bytes);
                    requestObject = (RequestObject)obj;
            } finally {
                    try {
                            baos.close();
                    } catch(Exception ex) {}
            }
            return requestObject;
    }

    private void sendData(SocketChannel socketChannel, ResponseObject responseObject) throws IOException {
            byte[] bytes = SerializableUtil.toBytes(responseObject);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            socketChannel.write(buffer);
    }
    
}


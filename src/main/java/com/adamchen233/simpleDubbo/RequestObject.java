/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adamchen233.simpleDubbo;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 *
 * @author xiaohui
 */
public class RequestObject implements Serializable{
    private static final long serialVersionUID = 1L;

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceMethod() {
        return serviceMethod;
    }

    public Object[] getArgs() {
        return args;
    }
    private final String serviceName;
    private final String serviceMethod;
    private final Object[] args;
    
    public RequestObject(String serviceName, String serviceMethod, Object[] args) {
        this.serviceName = serviceName;
        this.serviceMethod = serviceMethod;
        this.args = args;
    }
    
    

}

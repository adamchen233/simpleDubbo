/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adamchen233.simpleDubbo;

import java.io.Serializable;

/**
 *
 * @author xiaohui
 */
public class ResponseObject implements Serializable{
    private static final long serialVersionUID = 1L;
    private final Object resultObject;

    public Object getResultObject() {
        return resultObject;
    }

    public ResponseObject(Object resultObject) {
        this.resultObject = resultObject;
    }
}

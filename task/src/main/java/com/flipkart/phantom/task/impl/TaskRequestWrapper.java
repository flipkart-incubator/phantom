/*
 * Copyright 2012-2015, the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.phantom.task.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.flipkart.phantom.task.spi.RequestWrapper;

import java.util.Map;

/**
 *
 * <code>TaskRequestWrapper</code> has the data bytes and the parameters map for the Command protocol-request.
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public class TaskRequestWrapper<T> implements RequestWrapper {

    /** Data bytes */
    private byte[] data;

    /** Map of parameters */
    private Map<String,String> params;

    private JavaType javaType;

    /**Start Getter/Setter methods */

    public byte[] getData(){
        return data;
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public Map<String, String> getParams(){
        return params;
    }

    public void setParams(Map<String, String> params){
        this.params = params;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public void setJavaType(JavaType javaType) {
        this.javaType = javaType;
    }

    /**End Getter/Setter methods */

}

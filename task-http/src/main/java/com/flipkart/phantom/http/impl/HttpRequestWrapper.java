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

package com.flipkart.phantom.http.impl;

import com.flipkart.phantom.task.spi.RequestWrapper;

import java.util.List;
import java.util.Map;

/**
 *
 * <code>HttpRequestWrapper</code> has the the netty HttpRequest object for the  Http protocol-request.
 *
 * @author : arya.ketan
 * @version : 1.0
 * @date : 28/10/13
 */
public class HttpRequestWrapper implements RequestWrapper
{

    /** Data */
    private byte[] data;

    /** method */
    private String method;

    /** uri */
    private String uri;

    /** All header names and values that this message contains.*/
    private List<Map.Entry<String, String>> headers;

    /** * The protocol name of HTTP or its derived protocols, such as  */
    private String protocol;

    /** * The major version of HTTP or its derived protocols, such as  */
    private int majorVersion;

    /** * The minor version of HTTP or its derived protocols, such as  */
    private int minorVersion;

    /** Start Getter/Setter methods */

    public byte[] getData(){
        return data;
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public String getMethod(){
        return method;
    }

    public void setMethod(String method){
        this.method = method;
    }

    public String getUri(){
        return uri;
    }

    public void setUri(String uri){
        this.uri = uri;
    }

    public List<Map.Entry<String, String>> getHeaders(){
        return headers;
    }

    public void setHeaders(List<Map.Entry<String, String>> headers){
        this.headers = headers;
    }

    public String getProtocol(){
        return protocol;
    }

    public void setProtocol(String protocol){
        this.protocol = protocol;
    }

    public int getMajorVersion(){
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion){
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion(){
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion){
        this.minorVersion = minorVersion;
    }

     /**End Getter/Setter methods */
}

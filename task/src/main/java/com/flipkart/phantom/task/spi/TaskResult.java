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

package com.flipkart.phantom.task.spi;

import java.util.List;

/**
 * Result of a task handler invocation.
 *
 * @author devashishshankar
 * @version 1.0, 19 March, 2013
 */
public class TaskResult<T> {

    /** The default length in case the Object isn't a data array */
    private static final int DEFAULT_LENGTH = 0;

    private final boolean success;
    private final String message;
    private final T data;
    private final List<T> dataList;
    private int length = DEFAULT_LENGTH;
    private boolean profilingDone = false;
    private byte[] metadata;

    /** Various constructors for this class*/

    /**
     *
     * @param success Flag for the task execution
     * @param message Response Message
     */
    public TaskResult(boolean success, String message) {
        this(success, message, null);
    }

    /**
     *
     * @param success Flag for the task execution
     * @param message Response Message
     * @param data Response Data
     */
    public TaskResult(boolean success, String message,T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.dataList = null;
        if(data != null) {
            if(data instanceof byte[]) {
                byte[] dataBytes = (byte[]) data;
                this.length= dataBytes.length;
            } else {
                this.length = TaskResult.DEFAULT_LENGTH;
            }
        }
    }

    /**
     *
     * @param success Flag for the task execution
     * @param message Response Message
     * @param data Response Data
     * @param metadata metadata of the task result. To be sent before the data
     */
    public TaskResult(boolean success, String message,T data, byte[] metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.dataList = null;
        this.metadata = metadata;
        if(data != null) {
            if(data instanceof byte[]) {
                byte[] dataBytes = (byte[]) data;
                if(metadata != null) {
                    this.length= dataBytes.length + metadata.length;
                } else {
                    this.length= dataBytes.length;
                }

            } else {
                this.length = TaskResult.DEFAULT_LENGTH;
            }
        }
    }

    /**
     *
     * @param success Flag for the task execution
     * @param message Response Message
     * @param dataArray Response Data Array
     * @param length Data ArrayLength
     */
    public TaskResult(boolean success, String message, List<T> dataArray,int length) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.dataList = dataArray;
        this.length=length;
    }

    /**
     *
     * @param success Flag for the task execution
     * @param message Response Message
     * @param data Response Data
     * @param profilingDone whether Profiling has been done for the task execution
     */
    public TaskResult(boolean success, String message, T data, boolean profilingDone) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.dataList = null;
        if(data instanceof byte[]) {
            byte[] dataBytes = (byte[]) data;
            this.length=dataBytes.length;
        } else {
            this.length = TaskResult.DEFAULT_LENGTH;
        }
        this.profilingDone = profilingDone;
    }

    /**
     * Overriden super class method. Returns a string representation of this TaskResult
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("TaskResult[success:%s, message:%s,datalength:%d]", this.success, this.message, this.length);
    }

    /** Getter/Setter methods */

    public int getLength() {
        return length;
    }
    public boolean isDataArray() {
        return dataList != null;
    }
    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public Object getData() {
        return data;
    }
    public List<T> getDataArray() {
        return dataList;
    }
    public boolean isProfilingDone() {
        return profilingDone;
    }
    public byte[] getMetadata() {
        return metadata;
    }
    /** End Getter/Setter methods*/

}
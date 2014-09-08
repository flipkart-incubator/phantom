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
package com.flipkart.phantom.task.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.phantom.task.spi.TaskContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * User: srujan. K. V. S.
 * Date: 10/23/12
 * Time: 12:59 PM
 */
public class SystemUtils
{

	private static ObjectMapper objectMapper = new ObjectMapper();

	/* 
	 * Returns the hostname of the machine 
	 */
    public static String getHostName ()
    {
        String hostName = null;
        try
        {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e)
        {
            //TODO : Log the message into error log ?
        }
        return hostName;

    }

     public static String toJson(TaskContext taskContext, Object objectToBeEncoded)
    {
        try
        {
            ObjectMapper mapper = SystemUtils.getObjectMapper();
            return mapper.writeValueAsString(objectToBeEncoded);
        }
        catch(Exception ignored)
        {
            return "";
        }
    }

    public static Map fromJson(TaskContext taskContext, String objectToBeEncoded)
    {
        try
        {
            ObjectMapper mapper = SystemUtils.getObjectMapper();
            return  mapper.readValue(objectToBeEncoded, Map.class);
        }
        catch(Exception ignored)
        {
            return null;
        }
    }

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}

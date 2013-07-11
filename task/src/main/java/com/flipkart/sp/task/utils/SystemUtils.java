package com.flipkart.sp.task.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.flipkart.sp.task.spi.task.TaskContext;

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

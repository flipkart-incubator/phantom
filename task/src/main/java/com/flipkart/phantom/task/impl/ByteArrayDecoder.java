package com.flipkart.phantom.task.impl;


import com.flipkart.phantom.task.spi.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This is a ByteArrayDecoder which is the default implementation provided by phantom to decode
 * response of task handler execution
 */
public class ByteArrayDecoder implements Decoder<byte[]>
{
    Logger logger = LoggerFactory.getLogger(ByteArrayDecoder.class);

    @Override
    public byte[] decode(String s)
    {
        return s.getBytes();
    }

    @Override
    public byte[] decode(byte[] b)
    {
        return b;
    }

    @Override
    public byte[] decode(InputStream is)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append((line + "\n"));
            }
        } catch (IOException e) {
            logger.warn(e.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString().getBytes();
    }
}

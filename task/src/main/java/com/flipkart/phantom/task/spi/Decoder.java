package com.flipkart.phantom.task.spi;

import java.io.InputStream;

/**
 * @author : arya.ketan
 * @version : 1.0
 * @date : 10/09/14
 */
public interface Decoder<T>
{
    public T decode(String s);

    public T decode(byte[] b);

    public T decode(InputStream is);

}

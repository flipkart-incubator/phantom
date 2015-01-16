package com.flipkart.phantom.task.spi;

import java.io.InputStream;

/**
 * This interface is to provide clients the ability to decode responses
 * into the Object of their choice.
 * The default Implementation is provided by {@link com.flipkart.phantom.task.impl.ByteArrayDecoder}
 */
public interface Decoder<T>
{
    /**
     * @param s a String to Decode to Type T
     * @return T the type of Object to be decoded to
     * @throws  Exception exception that occurs
     */
    public T decode(String s) throws Exception;

    /**
     *
     * @param b a byte[] to Decode to Type T
     * @return T the type of Object to be decoded
     * @throws Exception exception that occurs
     */
    public T decode(byte[] b) throws Exception;

    /**
     * @param is inputStream to Decode to Type T
     * @return T the type of Object to be decoded to
     * @throws Exception exception that occurs
     */
    public T decode(InputStream is) throws Exception;

}

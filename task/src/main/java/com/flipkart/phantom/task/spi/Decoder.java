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
     */
    public T decode(String s);

    /**
     *
     * @param b a byte[] to Decode to Type T
     * @return T the type of Object to be decoded to
     */
    public T decode(byte[] b);

    /**
     * @param is inputStream to Decode to Type T
     * @return T the type of Object to be decoded to
     */
    public T decode(InputStream is);

}

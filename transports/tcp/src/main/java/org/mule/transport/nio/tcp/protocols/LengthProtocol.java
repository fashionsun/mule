/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.protocols;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transport.nio.tcp.io.ChannelInputStream;

/**
 * The LengthProtocol is an application level tcp protocol that can be used to
 * transfer large amounts of data without risking some data to be loss. The protocol
 * is defined by sending / reading an integer (the packet length) and then the data
 * to be transferred.
 * <p>
 * Note that use of this protocol must be symmetric - both the sending and receiving
 * connectors must use the same protocol.
 * </p>
 */
public class LengthProtocol extends DirectProtocol
{

    private static final Log logger = LogFactory.getLog(LengthProtocol.class);
    // TODO - can we not get this from the API somewhere?
    private static final int SIZE_INT = 4;
    public static final int NO_MAX_LENGTH = -1;
    private int maxMessageLength;

    public LengthProtocol()
    {
        this(NO_MAX_LENGTH);
    }

    public LengthProtocol(final int maxMessageLength)
    {
        super(NO_STREAM, SIZE_INT);
        this.setMaxMessageLength(maxMessageLength);
    }

    @Override
    public Object read(final InputStream is) throws IOException
    {
        // first read the data necessary to know the length of the payload
        final DataInputStream dis = new DataInputStream(is);
        try
        {
            final int length = dis.readInt();

            if (is instanceof ChannelInputStream)
            {
                ((ChannelInputStream) is).setExpectedBytes(SIZE_INT + length);
            }

            return readLengthBytes(length, dis);
        }
        catch (final EOFException eofe)
        {
            return null; // eof
        }
        catch (final InterruptedIOException iioe)
        {
            return null; // eof
        }
    }

    protected Object readLengthBytes(final int length, final DataInputStream dis) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("length: " + length);
        }

        if (length < 0 || (getMaxMessageLength() > 0 && length > getMaxMessageLength()))
        {
            throw new IOException("Length " + length + " exceeds limit: " + getMaxMessageLength());
        }

        // finally read the rest of the data
        final byte[] buffer = new byte[length];

        dis.readFully(buffer);
        if (logger.isDebugEnabled())
        {
            logger.debug("length read: " + buffer.length);
        }

        return buffer;
    }

    @Override
    protected void writeByteArray(final OutputStream os, final byte[] data) throws IOException
    {
        // Write the length and then the data.
        final DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(data.length);
        dos.write(data);
        // DataOutputStream size is SIZE_INT + the byte length, due to the writeInt
        // call
        // this should fix EE-1494
        if (dos.size() != data.length + SIZE_INT)
        {
            // only flush if the sizes don't match up
            dos.flush();
        }
    }

    /**
     * Read all four bytes for initial integer (limit is set in read)
     * 
     * @param len Amount transferred last call (-1 on EOF or socket error)
     * @param available Amount available
     * @return true if the transfer should continue
     */
    @Override
    protected boolean isRepeat(final int len, final int available)
    {
        return true;
    }

    public int getMaxMessageLength()
    {
        return maxMessageLength;
    }

    public void setMaxMessageLength(final int maxMessageLength)
    {
        this.maxMessageLength = maxMessageLength;
    }

}

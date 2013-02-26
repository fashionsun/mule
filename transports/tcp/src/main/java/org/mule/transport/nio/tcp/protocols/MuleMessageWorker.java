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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;

/**
 * Helper class for Mule message handling so that we can apply the same logic across
 * all sub-protocols (default, EOF and length).
 */
@SuppressWarnings("deprecation")
class MuleMessageWorker
{
    private static WireFormat wireFormat = new SerializedMuleMessageWireFormat();

    private MuleMessageWorker()
    {
        // no-op

    }

    public static byte[] doWrite() throws IOException
    {
        // TODO fix the api here so there is no need to use the RequestContext
        final DefaultMuleMessage msg = (DefaultMuleMessage) RequestContext.getEvent().getMessage();
        wireFormat.setMuleContext(RequestContext.getEvent().getMuleContext());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            wireFormat.write(baos, msg, msg.getEncoding());
        }
        catch (final MuleException e)
        {
            throw new IOException(e.getDetailedMessage());
        }
        return baos.toByteArray();
    }

    public static Object doRead(final Object message) throws IOException
    {
        if (message == null) return null;

        InputStream in;
        if (message instanceof byte[])
        {
            in = new ByteArrayInputStream((byte[]) message);
        }
        else
        {
            in = (InputStream) message;
        }

        try
        {
            return wireFormat.read(in);
        }
        catch (final MuleException e)
        {
            throw new IOException(e.getDetailedMessage());
        }

    }

}

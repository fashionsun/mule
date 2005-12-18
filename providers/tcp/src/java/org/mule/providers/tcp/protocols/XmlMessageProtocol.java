/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import org.mule.providers.tcp.TcpProtocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;

/**
 * <p>The XmlMessageProtocol is an application level tcp protocol that can be
 * used to read streaming xml documents.  The only requirement is that each
 * document include an xml declaration at the beginning of the document of the
 * form "<?xml ...".  In section 2.8, the xml 1.0 standard contains "Definition:
 * XML documents <strong>SHOULD</strong> begin with an XML declaration which
 * specifies the version of XML being used" while the xml 1.1 standard contains
 * "Definition: XML 1.1 documents <strong>MUST</strong> begin with an XML declaration
 * which specifies the version of XML being used".  The SHOULD indicates a
 * recommendation that, if not followed, needs to be carefully checked for
 * unintended consequences.  MUST indicates a mandatory requirement for a
 * well-formed document.  Please make sure that the xml documents being streamed
 * begin with an xml declaration when using this class.</p>
 * <p>Also, the default character encoding for the platform is used to decode the
 * message bytes when looking for the XML declaration.  Some caution with message
 * character encodings is warranted.</p>
 * <p>Finally, this class uses a PushbackInputStream to enable parsing of individual
 * messages.  The stream stores any pushed-back bytes into it's own internal buffer
 * and not the original stream.  Therefore, the read buffer size is intentionally
 * limited to insure that unread characters remain on the stream so that all data
 * may be read later.</p>
 *
 * @author <a href="mailto:rlucente@xecu.net">Rich Lucente</a>
 * @version $Revision$
 */
public class XmlMessageProtocol implements TcpProtocol
{
    private static String XML_PATTERN = "<?xml";
    private static int PUSHBACK_BUFFER_SIZE = 10;
    private static int READ_BUFFER_SIZE = 5;

    private static Map pbMap = new ConcurrentHashMap();

    /**
     * Adapted from DefaultProtocol
     *
     * @see DefaultProtocol#read(java.io.InputStream)
     */
    public byte[] read(InputStream is) throws IOException
    {
        // look for existing pushback wrapper for the given stream
        // if not found, create a wrapper
        PushbackInputStream pbis = (PushbackInputStream) pbMap.get(is);
        if (pbis == null) {
            pbis = new PushbackInputStream(is, PUSHBACK_BUFFER_SIZE);
            pbMap.put(is, pbis);
        }
        // read until xml pattern is seen (and then pushed back) or no more data
        // to read.  return all data as message
        ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        int patternIndex;
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int len;
        try {
            while ((len = pbis.read(buffer)) == 0) {
            }
        } catch (SocketException e) {
            return null;
        } catch (SocketTimeoutException e) {
            return null;
        }
        if (len == -1) {
            return null;
        } else {
            do {
                baos.write(buffer, 0, len);
                // start search at 2nd character in buffer (index=1) to
                // allow us to process message at start of buffer (index=0)
                patternIndex = baos.toString().indexOf(XML_PATTERN, 1);
                if (patternIndex > 0) {
                    break;
                }
                if (len < buffer.length) {
                    break;
                }
                int av = pbis.available();
                if (av == 0) {
                    break;
                }
            } while ((len = pbis.read(buffer)) > 0);

            baos.flush();
            baos.close();
            byte[] result = baos.toByteArray();

            if (patternIndex > 0) {
                // push back the start of the next message and fill the pushed-back
                // characters in the return buffer with whitespace
                pbis.unread(result, patternIndex, result.length - patternIndex);
                Arrays.fill(result, patternIndex, result.length, (byte) ' ');
            }

            return result;
        }
    }

    // simply write the data (which SHOULD begin with an XML declaration!)
    public void write(OutputStream os, byte[] data) throws IOException
    {
        os.write(data);
    }
}

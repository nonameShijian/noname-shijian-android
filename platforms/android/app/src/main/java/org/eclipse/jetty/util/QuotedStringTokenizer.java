//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util;

import java.io.IOException;
import java.util.Arrays;

/* ------------------------------------------------------------ */
/** StringTokenizer with Quoting support.
 *
 * This class is a copy of the java.util.StringTokenizer API and
 * the behaviour is the same, except that single and double quoted
 * string values are recognised.
 * Delimiters within quotes are not considered delimiters.
 * Quotes can be escaped with '\'.
 *
 * @see java.util.StringTokenizer
 *
 */
public class QuotedStringTokenizer
{
    /* ------------------------------------------------------------ */
    /** Quote a string.
     * The string is quoted only if quoting is required due to
     * embedded delimiters, quote characters or the
     * empty string.
     * @param s The string to quote.
     * @param delim the delimiter to use to quote the string
     * @return quoted string
     */
    public static String quoteIfNeeded(String s, String delim)
    {
        if (s==null)
            return null;
        if (s.length()==0)
            return "\"\"";


        for (int i=0;i<s.length();i++)
        {
            char c = s.charAt(i);
            if (c=='\\' || c=='"' || c=='\'' || Character.isWhitespace(c) || delim.indexOf(c)>=0)
            {
                StringBuffer b=new StringBuffer(s.length()+8);
                quote(b,s);
                return b.toString();
            }
        }

        return s;
    }

    private static final char[] escapes = new char[32];
    static
    {
        Arrays.fill(escapes, (char)0xFFFF);
        escapes['\b'] = 'b';
        escapes['\t'] = 't';
        escapes['\n'] = 'n';
        escapes['\f'] = 'f';
        escapes['\r'] = 'r';
    }

    /* ------------------------------------------------------------ */
    /** Quote a string into an Appendable.
     * The characters ", \, \n, \r, \t, \f and \b are escaped
     * @param buffer The Appendable
     * @param input The String to quote.
     */
    public static void quote(Appendable buffer, String input)
    {
        try
        {
            buffer.append('"');
            for (int i = 0; i < input.length(); ++i)
            {
                char c = input.charAt(i);
                if (c >= 32)
                {
                    if (c == '"' || c == '\\')
                        buffer.append('\\');
                    buffer.append(c);
                }
                else
                {
                    char escape = escapes[c];
                    if (escape == 0xFFFF)
                    {
                        // Unicode escape
                        buffer.append('\\').append('u').append('0').append('0');
                        if (c < 0x10)
                            buffer.append('0');
                        buffer.append(Integer.toString(c, 16));
                    }
                    else
                    {
                        buffer.append('\\').append(escape);
                    }
                }
            }
            buffer.append('"');
        }
        catch (IOException x)
        {
            throw new RuntimeException(x);
        }
    }
}

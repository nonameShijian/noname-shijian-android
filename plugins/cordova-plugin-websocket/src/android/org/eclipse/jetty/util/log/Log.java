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

package org.eclipse.jetty.util.log;

/**
 * Logging.
 *
 * Modified by KNOWLEDGECODE
 */
public class Log
{
    public static final String EXCEPTION = "EXCEPTION ";

    static int logLevel = android.util.Log.ERROR;

    /**
     * Obtain a named Logger based on the fully qualified class name.
     *
     * @param clazz
     *            the class to base the Logger name off of
     * @return the Logger with the given name
     */
    public static Logger getLogger(Class<?> clazz)
    {
        return getLogger(clazz.getName());
    }

    /**
     * Obtain a named Logger or the default Logger if null is passed.
     * @param name the Logger name
     * @return the Logger with the given name
     */
    public static Logger getLogger(String name)
    {
        return new AndroidLog(name == null ? "" : name);
    }

    /**
     * Change log level.
     * @param level
     */
    public static void setLogLevel(int level)
    {
        logLevel = level;
    }
}

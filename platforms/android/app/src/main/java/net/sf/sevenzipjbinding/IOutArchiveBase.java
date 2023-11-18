package net.sf.sevenzipjbinding;

import java.io.PrintStream;

/**
 * Base interface for compression/update archive main interfaces.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutArchiveBase {
    /**
     * Set alternative {@link PrintStream} for trace output. Default: {@link System#out}.
     * 
     * @param tracePrintStream
     *            instance of the {@link PrintStream}.
     * @see #setTrace(boolean)
     */
    public void setTracePrintStream(PrintStream tracePrintStream);

    /**
     * Alternative {@link PrintStream} for trace output. Default: {@link System#out}.
     * 
     * @return Alternative {@link PrintStream} for trace output
     * @see #setTrace(boolean)
     */
    public PrintStream getTracePrintStream();

    /**
     * If <code>true</code>, print trace message during compress and update operation to the {@link System#out} or
     * {@link #getTracePrintStream()}. Default: <code>false</code>
     * 
     * @param trace
     *            <code>true</code> - output trace messages, <code>false</code> be quite.
     * @see #setTracePrintStream(PrintStream)
     */
    public void setTrace(boolean trace);

    /**
     * If <code>true</code>, print trace message during compress and update operation to the {@link System#out} or
     * {@link #getTracePrintStream()}. Default: <code>false</code>
     * 
     * @return <code>true</code> - output trace messages, <code>false</code> be quite.
     * @see #setTracePrintStream(PrintStream)
     */
    public boolean isTrace();

}

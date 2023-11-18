package net.sf.sevenzipjbinding;

import java.io.Closeable;

/**
 * Interface used to operate with sequential input stream.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public interface ISequentialInStream extends Closeable {
    /**
     * Reads at least 1 and maximum <code>data.length</code> bytes from the in-stream. If <code>data.length == 0</code>
     * 0 should be returned. If <code>data.length != 0</code>, then return value 0 indicates end-of-stream (EOF). This
     * means no more bytes can be read from the stream. <br>
     * This function is allowed to read less than number of remaining bytes in stream and less then
     * <code>data.length</code>. You must call <code>read()</code> function in loop, if you need exact amount of data.<br>
     * <br>
     * <i>Note:</i> depending on the archive format and the data size this method may be called from different threads.
     * Synchronized implementation may be required.
     * 
     * 
     * @param data
     *            buffer to get read data
     * 
     * @return amount of bytes written in the <code>data</code> array. 0 - represents end of stream.
     * 
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public int read(byte[] data) throws SevenZipException;
}

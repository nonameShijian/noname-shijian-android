package net.sf.sevenzipjbinding;

/**
 * OutStream interface used to operate with seekable output streams (random access streams)
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutStream extends ISequentialOutStream, ISeekableStream {
    /**
     * Set new size for the out stream.<br>
     * <br>
     * <i>Note:</i> depending on the archive format and the data size this method may be called from different threads.
     * Synchronized implementation may be required.
     * 
     * 
     * @param newSize
     *            new size
     * 
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setSize(long newSize) throws SevenZipException;
}

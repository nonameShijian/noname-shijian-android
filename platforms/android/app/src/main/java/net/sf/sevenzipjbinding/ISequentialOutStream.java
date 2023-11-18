package net.sf.sevenzipjbinding;

/**
 * Interface used to operate with sequential output stream.
 *
 * @author Boris Brodski
 * @since 4.65-1
 */
public interface ISequentialOutStream {
    /**
     * Write <code>data</code> byte array to the stream. If {@code data.length > 0} this function must write at least 1
     * byte. This function is allowed to write less than <code>data.length</code> bytes. You must call Write function in
     * loop, if you need to write exact amount of data.<br>
     * <br>
     * <i>Note:</i> depending on the archive format and the data size this method may be called from different threads.
     * Synchronized implementation may be required.
     *
     * @param data
     *            data to write
     * @return count of written bytes
     *
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public int write(byte[] data) throws SevenZipException;
}

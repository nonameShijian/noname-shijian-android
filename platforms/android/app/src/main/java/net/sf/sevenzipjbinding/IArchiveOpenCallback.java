package net.sf.sevenzipjbinding;

/**
 * Interface to receive information about open archive operation for extraction or update.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public interface IArchiveOpenCallback {

    /**
     * Set total amount of work to be done. Both parameter are optional.
     * 
     * @param files
     *            count of files to be processed (optional)
     * @param bytes
     *            count of bytes to be processed (optional)
     * 
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setTotal(Long files, Long bytes) throws SevenZipException;

    /**
     * Set amount of competed work. Both parameter are optional.
     * 
     * @param files
     *            count of processed files (optional)
     * @param bytes
     *            count of processed bytes (optional)
     * 
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setCompleted(Long files, Long bytes) throws SevenZipException;
}

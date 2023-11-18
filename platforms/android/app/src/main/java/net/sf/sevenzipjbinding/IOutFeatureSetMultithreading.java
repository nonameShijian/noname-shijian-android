package net.sf.sevenzipjbinding;

/**
 * Feature interface for the setting 'maximal number of threads' to use during compression or update operation. Use
 * {@link SevenZip#openOutArchive(ArchiveFormat)} to get implementation of this interface.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutFeatureSetMultithreading {

    /**
     * Set number of threads to use.
     * 
     * @param threadCount
     *            number of threads to use,<br>
     *            <code>0</code> - match count of threads to the count of the available processors,<br>
     *            <code>-1</code> - use default value
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setThreadCount(int threadCount) throws SevenZipException;
}

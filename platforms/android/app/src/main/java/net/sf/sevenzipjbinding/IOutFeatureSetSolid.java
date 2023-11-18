package net.sf.sevenzipjbinding;

/**
 * 
 * Feature interface for the setting 'solid'. Use {@link SevenZip#openOutArchive(ArchiveFormat)} to get implementation
 * of this interface.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutFeatureSetSolid {

    /**
     * Set solid archive option. If deactivated, some other solid options like 'solid files' will take no effect.
     * 
     * @param solid
     *            <code>true</code> - use default configuration, <code>false</code> - deactivate solid block mode
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setSolid(boolean solid) throws SevenZipException;

    /**
     * Put specified count of files in a single solid block. See {@link #setSolid(boolean)}.
     * 
     * @see #setSolid(boolean)
     * @param countOfFilesPerBlock
     *            count of files per solid block. <code>-1</code> - use default configuration
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setSolidFiles(int countOfFilesPerBlock) throws SevenZipException;

    /**
     * Put specified count of bytes in a single solid block. See {@link #setSolid(boolean)}.
     * 
     * @see #setSolid(boolean)
     * @param countOfBytesPerBlock
     *            count of bytes per solid block. <code>-1</code> - use default configuration.
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setSolidSize(long countOfBytesPerBlock) throws SevenZipException;

    /**
     * Put all files with the same extension following each other in the order of packing in a solid block. See
     * {@link #setSolid(boolean)}.
     * 
     * @see #setSolid(boolean)
     * @param solidExtension
     *            <code>true</code> - activate this feature, <code>false</code> - use default configuration
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setSolidExtension(boolean solidExtension) throws SevenZipException;
}

package net.sf.sevenzipjbinding;

/**
 * Feature interface for the setting 'compression level'. Use {@link SevenZip#openOutArchive(ArchiveFormat)} or one of
 * the <code>SevenZip.openOutArchiveXxx()</code> methods to get implementation of this interface.
 * 
 * @see IOutCreateArchive
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 * 
 */
public interface IOutFeatureSetLevel {

    /**
     * Set compression level:
     * <ul>
     * <li>0 - Copy mode (no compression)
     * <li>1 - Fastest
     * <li>3 - Fast
     * <li>5 - Normal
     * <li>7 - Maximum
     * <li>9 - Ultra
     * </ul>
     * Note, that the meaning of compression level can differ through out different archive formats.
     * 
     * @param compressionLevel
     *            compression level to set. <code>-1</code> - use default
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setLevel(int compressionLevel) throws SevenZipException;
}

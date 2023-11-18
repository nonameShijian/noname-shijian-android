package net.sf.sevenzipjbinding;

/**
 * Feature interface for the setting 'encrypt header'. Use {@link SevenZip#openOutArchive(ArchiveFormat)} or one of the
 * <code>SevenZip.openOutArchiveXxx()</code> methods to get implementation of this interface.
 *
 * @see IOutCreateArchive
 *
 * @author Boris Brodski
 * @since 16.02-2.01
 *
 */
public interface IOutFeatureSetEncryptHeader {

    /**
     * Enable/disable header encryption for password protected archives. If enabled, the archive header containing file
     * names and other metadata will be encrypted as well.<br>
     * <br>
     * <i>Note:</i> Implement {@link ICryptoGetTextPassword} in your callback class to turn on the encryption.
     *
     * @param enabled
     *            <code>true</code> enable header encryption, otherwise <code>false</code>
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setHeaderEncryption(boolean enabled) throws SevenZipException;
}

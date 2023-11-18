package net.sf.sevenzipjbinding;

/**
 * Interface to provide password to the archive engine.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public interface ICryptoGetTextPassword {
    /**
     * Returns password
     * 
     * @return password
     * 
     * 
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public String cryptoGetTextPassword() throws SevenZipException;
}

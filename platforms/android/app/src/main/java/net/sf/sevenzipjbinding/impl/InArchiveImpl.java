package net.sf.sevenzipjbinding.impl;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.IOutItemAllFormats;
import net.sf.sevenzipjbinding.IOutItemBZip2;
import net.sf.sevenzipjbinding.IOutItemBase;
import net.sf.sevenzipjbinding.IOutItemGZip;
import net.sf.sevenzipjbinding.IOutItemTar;
import net.sf.sevenzipjbinding.IOutItemZip;
import net.sf.sevenzipjbinding.IOutUpdateArchive;
import net.sf.sevenzipjbinding.IOutUpdateArchive7z;
import net.sf.sevenzipjbinding.IOutUpdateArchiveBZip2;
import net.sf.sevenzipjbinding.IOutUpdateArchiveGZip;
import net.sf.sevenzipjbinding.IOutUpdateArchiveTar;
import net.sf.sevenzipjbinding.IOutUpdateArchiveZip;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.NFileTimeType;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.PropertyInfo;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.impl.SimpleInArchiveImpl;

/**
 * Implementation of {@link IInArchive}.
 *
 * @author Boris Brodski
 * @since 4.65-1
 */
//TODO null check all parameters: If null slips through into native code there will be no NPE :(
public final class InArchiveImpl implements IInArchive {
    private static class ExtractSlowCallback implements IArchiveExtractCallback {
        ISequentialOutStream sequentialOutStreamParam;
        private ExtractOperationResult extractOperationResult;

        ExtractSlowCallback(ISequentialOutStream sequentialOutStream) {
            this.sequentialOutStreamParam = sequentialOutStream;
        }

        /**
         * {@inheritDoc}
         */
        public void setTotal(long total) {
        }

        /**
         * {@inheritDoc}
         */
        public void setCompleted(long completeValue) {
        }

        /**
         * {@inheritDoc}
         */
        public void setOperationResult(ExtractOperationResult extractOperationResult) {
            this.extractOperationResult = extractOperationResult;
        }

        /**
         * {@inheritDoc}
         */
        public void prepareOperation(ExtractAskMode extractAskMode) {
        }

        /**
         * {@inheritDoc}
         */
        public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) {
            return extractAskMode.equals(ExtractAskMode.EXTRACT) ? sequentialOutStreamParam : null;
        }

        ExtractOperationResult getExtractOperationResult() {
            return extractOperationResult;
        }
    }

    private static final class ExtractSlowCryptoCallback extends ExtractSlowCallback implements ICryptoGetTextPassword {

        private String password;

        public ExtractSlowCryptoCallback(ISequentialOutStream sequentialOutStream, String password) {
            super(sequentialOutStream);
            this.password = password;
        }

        public String cryptoGetTextPassword() throws SevenZipException {
            return password;
        }
    }

    private long jbindingSession;
    private long sevenZipArchiveInstance;
    private long sevenZipInStreamInstance;
    private OutArchiveImpl<?> outArchiveImpl;

    private int numberOfItems = -1;

    private ArchiveFormat archiveFormat;
    private boolean closed;

    /**
     * {@inheritDoc}
     */
    public void extract(int[] indices, boolean testMode, IArchiveExtractCallback extractCallback)
            throws SevenZipException {
        ensureOpened();
        nativeExtract(indices, testMode, extractCallback);
    }

    /**
     * {@inheritDoc}
     */
    public ExtractOperationResult extractSlow(int index, ISequentialOutStream outStream) throws SevenZipException {
        ensureOpened();
        ExtractSlowCallback extractCallback = new ExtractSlowCallback(outStream);
        nativeExtract(new int[] { index }, false, extractCallback);
        return extractCallback.getExtractOperationResult();
    }

    /**
     * {@inheritDoc}
     */

    public ExtractOperationResult extractSlow(int index, ISequentialOutStream outStream, String password)
            throws SevenZipException {
        ensureOpened();
        ExtractSlowCryptoCallback extractCallback = new ExtractSlowCryptoCallback(outStream, password);
        nativeExtract(new int[] { index }, false, extractCallback);
        return extractCallback.getExtractOperationResult();
    }

    private native void nativeExtract(int[] indices, boolean testMode, IArchiveExtractCallback extractCallback)
            throws SevenZipException;

    private native Object nativeGetArchiveProperty(int propID) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public Object getArchiveProperty(PropID propID) throws SevenZipException {
        ensureOpened();
        return nativeGetArchiveProperty(propID.getPropIDIndex());
    }

    private native String nativeGetStringArchiveProperty(int propID) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public String getStringArchiveProperty(PropID propID) throws SevenZipException {
        ensureOpened();
        return nativeGetStringArchiveProperty(propID.getPropIDIndex());
    }

    private native PropertyInfo nativeGetArchivePropertyInfo(int index) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public PropertyInfo getArchivePropertyInfo(int index) throws SevenZipException {
        ensureOpened();
        return nativeGetArchivePropertyInfo(index);
    }

    private native int nativeGetNumberOfArchiveProperties() throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public int getNumberOfArchiveProperties() throws SevenZipException {
        ensureOpened();
        return nativeGetNumberOfArchiveProperties();
    }

    private native int nativeGetNumberOfProperties() throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public int getNumberOfProperties() throws SevenZipException {
        ensureOpened();
        return nativeGetNumberOfProperties();
    }

    private native PropertyInfo nativeGetPropertyInfo(int index) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public PropertyInfo getPropertyInfo(int index) throws SevenZipException {
        ensureOpened();
        return nativeGetPropertyInfo(index);
    }

    private native void nativeClose() throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public void close() throws SevenZipException {
        if (closed) {
            return;
        }
        closed = true;
        nativeClose();
    }

    private native int nativeGetNumberOfItems() throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public int getNumberOfItems() throws SevenZipException {
        ensureOpened();
        if (numberOfItems == -1) {
            numberOfItems = nativeGetNumberOfItems();
        }
        return numberOfItems;
    }

    private native Object nativeGetProperty(int index, int propID) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public Object getProperty(int index, PropID propID) throws SevenZipException {
        ensureOpened();
        if (index < 0 || index >= getNumberOfItems()) {
            throw new SevenZipException(
                    "Index out of range. Index: " + index + ", NumberOfItems: " + getNumberOfItems());
        }
        // Correct some returned values
        Object returnValue = nativeGetProperty(index, propID.getPropIDIndex());
        switch (propID) {
        case SIZE:
        case PACKED_SIZE:
            // ARJ archive returns integer sizes (32 bit).
            // Correcting it here, since every other archive returns Long (64 bit).
            if (returnValue instanceof Integer) {
                return Long.valueOf(((Integer) returnValue).longValue());
            }

            if (returnValue == null && archiveFormat != null && archiveFormat == ArchiveFormat.NSIS) {
                return Long.valueOf(0);
            }
            break;
        case IS_FOLDER:
            // Some stream archive formats doesn't set this property.
            if (returnValue == null) {
                return Boolean.FALSE;
            }
            break;
        case ENCRYPTED:
            // Some stream archive formats doesn't set this property either.
            if (returnValue == null) {
                return Boolean.FALSE;
            }
            break;
        case TIME_TYPE:
            if (returnValue != null) {
                return NFileTimeType.values()[((Integer) returnValue).intValue()];
            }
            break;
        }
        return returnValue;
    }

    private native String nativeGetStringProperty(int index, int propID) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public String getStringProperty(int index, PropID propID) throws SevenZipException {
        ensureOpened();
        if (index < 0 || index >= getNumberOfItems()) {
            throw new SevenZipException(
                    "Index out of range. Index: " + index + ", NumberOfItems: " + getNumberOfItems());
        }
        return nativeGetStringProperty(index, propID.getPropIDIndex());
    }

    /**
     * {@inheritDoc}
     */
    public ISimpleInArchive getSimpleInterface() {
        return new SimpleInArchiveImpl(this);
    }

    /**
     * ${@inheritDoc}
     */
    public ArchiveFormat getArchiveFormat() {
        return archiveFormat;
    }

    /**
     * Set archive format of the opened archive. This method should be called only through JNI.
     *
     * @param archiveFormat
     *            format of the opened archive
     */
    private void setArchiveFormat(String archiveFormatString) {
        for (ArchiveFormat archiveFormat : ArchiveFormat.values()) {
            if (archiveFormat.getMethodName().equalsIgnoreCase(archiveFormatString)) {
                this.archiveFormat = archiveFormat;
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public IOutUpdateArchive<IOutItemAllFormats> getConnectedOutArchive() throws SevenZipException {
        ensureOpened();
        return getConnectedOutArchiveIntern();
    }

    @SuppressWarnings("unchecked")
    private <T extends IOutItemBase> IOutUpdateArchive<T> getConnectedOutArchiveIntern() throws SevenZipException {
        if (outArchiveImpl == null) {
            createConnectedOutArchive();
        }
        return (IOutUpdateArchive<T>) outArchiveImpl;
    }

    /**
     * {@inheritDoc}
     */
    public IOutUpdateArchive7z getConnectedOutArchive7z() throws SevenZipException {
        ensureOpened();
        ensureArchiveFormatForArchiveFormatSpecificUpdateAPI(ArchiveFormat.SEVEN_ZIP);
        return (IOutUpdateArchive7z) (this.<IOutItem7z> getConnectedOutArchiveIntern());
    }

    /**
     * {@inheritDoc}
     */
    public IOutUpdateArchiveZip getConnectedOutArchiveZip() throws SevenZipException {
        ensureOpened();
        ensureArchiveFormatForArchiveFormatSpecificUpdateAPI(ArchiveFormat.ZIP);
        return (IOutUpdateArchiveZip) (this.<IOutItemZip> getConnectedOutArchiveIntern());
    }

    /**
     * {@inheritDoc}
     */
    public IOutUpdateArchiveTar getConnectedOutArchiveTar() throws SevenZipException {
        ensureOpened();
        ensureArchiveFormatForArchiveFormatSpecificUpdateAPI(ArchiveFormat.TAR);
        return (IOutUpdateArchiveTar) (this.<IOutItemTar> getConnectedOutArchiveIntern());
    }

    /**
     * {@inheritDoc}
     */
    public IOutUpdateArchiveGZip getConnectedOutArchiveGZip() throws SevenZipException {
        ensureOpened();
        ensureArchiveFormatForArchiveFormatSpecificUpdateAPI(ArchiveFormat.GZIP);
        return (IOutUpdateArchiveGZip) (this.<IOutItemGZip> getConnectedOutArchiveIntern());
    }

    /**
     * {@inheritDoc}
     */
    public IOutUpdateArchiveBZip2 getConnectedOutArchiveBZip2() throws SevenZipException {
        ensureOpened();
        ensureArchiveFormatForArchiveFormatSpecificUpdateAPI(ArchiveFormat.BZIP2);
        return (IOutUpdateArchiveBZip2) (this.<IOutItemBZip2> getConnectedOutArchiveIntern());
    }

    private void ensureArchiveFormatForArchiveFormatSpecificUpdateAPI(ArchiveFormat archiveFormat)
            throws SevenZipException {
        if (getArchiveFormat() != archiveFormat) {
            throw new SevenZipException("Archive format specific update API for " + archiveFormat.getMethodName()
                    + "-archives can't work with the currently opened " + getArchiveFormat().getMethodName()
                    + "-archive");
        }
    }

    private void createConnectedOutArchive() throws SevenZipException {
        if (!archiveFormat.isOutArchiveSupported()) {
            throw new IllegalStateException(
                    "Archive format '" + archiveFormat + "' doesn't support archive manipulations.");
        }

        try {
            outArchiveImpl = archiveFormat.getOutArchiveImplementation().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Internal error: Can't create new instance of the class "
                    + archiveFormat.getOutArchiveImplementation() + " using default constructor.");
        }
        outArchiveImpl.setInArchive(this);
        outArchiveImpl.setArchiveFormat(archiveFormat);

        nativeConnectOutArchive(outArchiveImpl, archiveFormat);
    }

    void ensureOpened() throws SevenZipException {
        if (closed) {
            throw new SevenZipException("InArchive closed");
        }
    }

    private native void nativeConnectOutArchive(OutArchiveImpl<?> outArchiveImpl, ArchiveFormat archiveFormat)
            throws SevenZipException;
}

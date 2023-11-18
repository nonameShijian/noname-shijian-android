package net.sf.sevenzipjbinding.impl;

import java.io.IOException;
import java.io.PrintStream;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IOutArchive;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItemBase;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Common archive create and update class.
 *
 * @param <T>
 *            the type of the item callback implementation
 *
 * @see OutArchive7zImpl
 * @see OutArchiveZipImpl
 * @see OutArchiveGZipImpl
 * @see OutArchiveBZip2Impl
 * @see OutArchiveTarImpl
 *
 * @author Boris Brodski
 * @since 9.20-2.00
 */
// TODO null check all parameters: If null slips through into native code there will be no NPE :(
public class OutArchiveImpl<T extends IOutItemBase> implements IOutArchive<T> {

    private long jbindingSession;
    private long sevenZipArchiveInstance;

    /**
     * Instance of {@link IInArchive} for connected OutArchives.
     */
    private IInArchive inArchive;

    /**
     * Archive format enum value.
     */
    private ArchiveFormat archiveFormat;

    private Boolean headerEncryption;
    private int compressionLevel = -1;
    private int threadCount = -1;
    private PrintStream tracePrintStream;
    private boolean trace; // Read by native code
    private boolean closed;

    protected void setInArchive(IInArchive inArchive) {
        this.inArchive = inArchive;
    }

    protected void featureSetHeaderEncryption(boolean enabled) {
        this.headerEncryption = Boolean.valueOf(enabled);
    }

    protected void featureSetLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    protected void featureSetThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    protected void applyFeatures() throws SevenZipException {
        ensureOpened();
        if (compressionLevel != -1) {
            nativeSetLevel(compressionLevel);
        }
        if (headerEncryption != null) {
            nativeSetHeaderEncryption(headerEncryption.booleanValue());
        }

        if (threadCount >= 0) {
            nativeSetMultithreading(threadCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ArchiveFormat getArchiveFormat() {
        return archiveFormat;
    }

    protected void setArchiveFormat(ArchiveFormat archiveFormat) {
        this.archiveFormat = archiveFormat;
    }

    protected native void nativeSetHeaderEncryption(boolean encryptHeader) throws SevenZipException;

    protected native void nativeSetLevel(int compressionLevel) throws SevenZipException;

    /**
     * Set solid features.
     *
     * @param solidBlockSpec
     *            <code>null</code> - turn solid off
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    protected native void nativeSetSolidSpec(String solidBlockSpec) throws SevenZipException;

    protected native void nativeSetMultithreading(int threadCount) throws SevenZipException;

    private native void nativeUpdateItems(ISequentialOutStream outStream, int numberOfItems,
            Object archiveUpdateCallback) throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (inArchive != null) {
            return; // In case of connected OutArchive no explicit closing necessary.
        }
        nativeClose();
    }

    private native void nativeClose() throws SevenZipException;

    /**
     * {@inheritDoc}
     */
    public void updateItems(ISequentialOutStream outStream, int numberOfItems, IOutCreateCallback<T> outUpdateCallback)
            throws SevenZipException {
        ensureOpened();
        doUpdateItems(outStream, numberOfItems, outUpdateCallback);
    }

    private void doUpdateItems(ISequentialOutStream outStream, int numberOfItems,
            IOutCreateCallback<?> archiveCreateCallback) throws SevenZipException {
        applyFeatures();
        nativeUpdateItems(outStream, numberOfItems, archiveCreateCallback);
    }

    /**
     * {@inheritDoc}
     */
    public void createArchive(ISequentialOutStream outStream, int numberOfItems,
            IOutCreateCallback<? extends T> outCreateCallback) throws SevenZipException {
        ensureOpened();
        doUpdateItems(outStream, numberOfItems, outCreateCallback);
    }

    /**
     * {@inheritDoc}
     */
    public IInArchive getConnectedInArchive() {
        return inArchive;
    }

    // Called by native code
    private final void traceMessage(String message) {
        if (trace) {
            if (tracePrintStream == null) {
                System.out.println(message);
            } else {
                tracePrintStream.println(message);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTracePrintStream(PrintStream tracePrintStream) {
        this.tracePrintStream = tracePrintStream;
    }

    /**
     * {@inheritDoc}
     */
    public PrintStream getTracePrintStream() {
        return tracePrintStream;
    }

    /**
     * {@inheritDoc}
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTrace() {
        return trace;
    }

    private void ensureOpened() throws SevenZipException {
        if (closed) {
            throw new SevenZipException("OutArchive closed");
        }
        if (inArchive != null) {
            ((InArchiveImpl) inArchive).ensureOpened();
        }
    }
}

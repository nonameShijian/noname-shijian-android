package net.sf.sevenzipjbinding.simple.impl;

import java.util.Date;

import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

/**
 * Standard implementation of {@link ISimpleInArchiveItem}, simplified 7-Zip-JBinding interface.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public class SimpleInArchiveItemImpl implements ISimpleInArchiveItem {

    private final SimpleInArchiveImpl simpleInArchiveImpl;
    private final int index;

    /**
     * Create instance of {@link SimpleInArchiveItemImpl} representing archive item with index index<code>index</code>
     * of archive <code>simpleInArchiveImpl</code>.
     * 
     * @param simpleInArchiveImpl
     *            opened archive
     * @param index
     *            index of the item in archive
     */
    public SimpleInArchiveItemImpl(SimpleInArchiveImpl simpleInArchiveImpl, int index) {
        this.simpleInArchiveImpl = simpleInArchiveImpl;
        this.index = index;
    }

    /**
     * Create instance of {@link SimpleInArchiveItemImpl} representing archive item with index index<code>index</code>
     * of archive <code>sevenZipInArchive</code>.
     * 
     * @param sevenZipInArchive
     *            opened archive
     * @param index
     *            index of the item in archive
     */
    public SimpleInArchiveItemImpl(IInArchive sevenZipInArchive, int index) {
        this.simpleInArchiveImpl = new SimpleInArchiveImpl(sevenZipInArchive);
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */

    public String getPath() throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getStringProperty(index, PropID.PATH);
    }

    /**
     * {@inheritDoc}
     */

    public Integer getAttributes() throws SevenZipException {
        return (Integer) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.ATTRIBUTES);
    }

    /**
     * {@inheritDoc}
     */

    public Integer getCRC() throws SevenZipException {
        return (Integer) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.CRC);
    }

    /**
     * {@inheritDoc}
     */

    public String getComment() throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getStringProperty(index, PropID.COMMENT);
    }

    /**
     * {@inheritDoc}
     */

    public Date getCreationTime() throws SevenZipException {
        return (Date) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.CREATION_TIME);
    }

    /**
     * {@inheritDoc}
     */

    public String getGroup() throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getStringProperty(index, PropID.GROUP);
    }

    /**
     * {@inheritDoc}
     */

    public String getHostOS() throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getStringProperty(index, PropID.HOST_OS);
    }

    /**
     * {@inheritDoc}
     */

    public Date getLastAccessTime() throws SevenZipException {
        return (Date) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.LAST_ACCESS_TIME);
    }

    /**
     * {@inheritDoc}
     */

    public Date getLastWriteTime() throws SevenZipException {
        return (Date) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.LAST_MODIFICATION_TIME);
    }

    /**
     * {@inheritDoc}
     */

    public String getMethod() throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getStringProperty(index, PropID.METHOD);
    }

    /**
     * {@inheritDoc}
     */

    public Long getPackedSize() throws SevenZipException {
        return (Long) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.PACKED_SIZE);
    }

    /**
     * {@inheritDoc}
     */

    public Integer getPosition() throws SevenZipException {
        return (Integer) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.POSITION);
    }

    /**
     * {@inheritDoc}
     */

    public Long getSize() throws SevenZipException {
        return (Long) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.SIZE);
    }

    /**
     * {@inheritDoc}
     */

    public String getUser() throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getStringProperty(index, PropID.USER);
    }

    /**
     * {@inheritDoc}
     */

    public Boolean isCommented() throws SevenZipException {
        return (Boolean) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.COMMENTED);
    }

    /**
     * {@inheritDoc}
     */

    public boolean isEncrypted() throws SevenZipException {
        return ((Boolean) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.ENCRYPTED))
                .booleanValue();
    }

    /**
     * {@inheritDoc}
     */

    public boolean isFolder() throws SevenZipException {
        return ((Boolean) simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().getProperty(index, PropID.IS_FOLDER))
                .booleanValue();
    }

    public ExtractOperationResult extractSlow(ISequentialOutStream outStream) throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().extractSlow(index, outStream);
    }

    public ExtractOperationResult extractSlow(ISequentialOutStream outStream, String password) throws SevenZipException {
        return simpleInArchiveImpl.testAndGetSafeSevenZipInArchive().extractSlow(index, outStream, password);
    }

    /**
     * {@inheritDoc}
     */

    public int getItemIndex() {
        return index;
    }
}

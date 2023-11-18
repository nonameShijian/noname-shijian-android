package net.sf.sevenzipjbinding.simple.impl;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

/**
 * Standard implementation of {@link ISimpleInArchive}, simplified 7-Zip-JBinding interface.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public class SimpleInArchiveImpl implements ISimpleInArchive {
    private final IInArchive sevenZipInArchive;
    private boolean wasClosed = false;

    /**
     * Constructing an instance of {@link SimpleInArchiveImpl} from a instance of {@link IInArchive}.
     * 
     * @param sevenZipInArchive
     *            a base instance of {@link IInArchive}
     */
    public SimpleInArchiveImpl(IInArchive sevenZipInArchive) {
        this.sevenZipInArchive = sevenZipInArchive;
    }

    /**
     * {@inheritDoc}
     */

    public void close() throws SevenZipException {
        sevenZipInArchive.close();
        wasClosed = true;
    }

    /**
     * {@inheritDoc}
     */

    public ISimpleInArchiveItem[] getArchiveItems() throws SevenZipException {
        ISimpleInArchiveItem[] result = new ISimpleInArchiveItem[getNumberOfItems()];
        for (int i = 0; i < result.length; i++) {
            result[i] = new SimpleInArchiveItemImpl(this, i);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */

    public int getNumberOfItems() throws SevenZipException {
        return testAndGetSafeSevenZipInArchive().getNumberOfItems();
    }

    /**
     * Tests, if 7-Zip In archive interface can be accessed safely.
     * 
     * @return 7-Zip In archive interface
     * @throws SevenZipException
     *             archive can't be accessed any more
     */
    public IInArchive testAndGetSafeSevenZipInArchive() throws SevenZipException {
        if (wasClosed) {
            throw new SevenZipException("Archive was closed");
        }
        return sevenZipInArchive;
    }

    /**
     * ${@inheritDoc}
     */
    public ISimpleInArchiveItem getArchiveItem(int index) throws SevenZipException {
        if (index < 0 || index >= sevenZipInArchive.getNumberOfItems()) {
            throw new SevenZipException("Index " + index + " is out of range. Number of items in archive: "
                    + sevenZipInArchive.getNumberOfItems());
        }
        return new SimpleInArchiveItemImpl(this, index);
    }
}

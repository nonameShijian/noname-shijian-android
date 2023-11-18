package net.sf.sevenzipjbinding.simple;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Simplified interface for C++ <code>IInArchive</code>. For binding of original 7-Zip C++ interface see
 * {@link IInArchive}.
 * 
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public interface ISimpleInArchive {
    /**
     * Close archive. No more archive operations are possible.<br>
     * <b>Note</b>: This method should be always called to free system resources.
     * 
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public void close() throws SevenZipException;

    /**
     * Return count of items in archive.
     * 
     * @return count of item in archive.
     * 
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public int getNumberOfItems() throws SevenZipException;

    /**
     * Return array of archive items with {@link #getNumberOfItems()} elements.
     * 
     * @return array of archive items
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public ISimpleInArchiveItem[] getArchiveItems() throws SevenZipException;

    /**
     * Return a simple representation of the archive item with index <code>index</code>.
     * 
     * @param index
     *            index of the archive item to return
     * @return corresponding instance of {@link ISimpleInArchiveItem}
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public ISimpleInArchiveItem getArchiveItem(int index) throws SevenZipException;
}

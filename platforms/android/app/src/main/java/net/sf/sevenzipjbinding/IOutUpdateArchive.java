package net.sf.sevenzipjbinding;

/**
 * The interface provides functionality to update an existing archive. The standard way to get the implementation is to
 * use {@link IInArchive#getConnectedOutArchive()} method like this:<br>
 * <br>
 *
 * <pre>
 *  {@link IInArchive} inArchive = {@link SevenZip}.openInArchive({@link ArchiveFormat#SEVEN_ZIP}, inStream);
 *  {@link IOutUpdateArchive}{@code <}{@link IOutItem7z}{@code >} outArchive = inArchive.openOutArchive();
 *
 *  if (outArchive instanceof {@link IOutFeatureSetLevel}) {
 *      (({@link IOutFeatureSetLevel})outArchive).setLevel(myLevel);
 *  }
 *
 *  outArchive.updateItems(...);
 *  ...
 *
 *  inArchive.close();
 * </pre>
 *
 * <br>
 * No explicit closing is necessary. Connected out-archive get closed automatically when corresponding in-archive get
 * closed.
 *
 * @param <T>
 *            the type of the corresponding archive item data class (out item), like {@link IOutItem7z} or
 *            {@link IOutItemZip}. Use {@link IOutItemAllFormats} interface to support all available archive formats.
 *
 * @see IInArchive
 * @see IOutItemBase
 * @see IOutItemAllFormats
 *
 * @author Boris Brodski
 * @since 9.20-2.0
 *
 */
public interface IOutUpdateArchive<T extends IOutItemBase> extends IOutArchiveBase {

    /**
     * Update items in archive (actually creating a new one based on the old one).
     *
     * @param outStream
     *            output stream to get the new archive
     * @param numberOfItems
     *            number of items in the new archive
     * @param outCreateCallback
     *            create call back object to provide more information for archive update operation.
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public void updateItems(ISequentialOutStream outStream, int numberOfItems, IOutCreateCallback<T> outCreateCallback)
            throws SevenZipException;

    /**
     * Return archive format used with this instance of {@link IOutStream}
     *
     * @return archive format used with this instance of {@link IOutStream}
     */
    public ArchiveFormat getArchiveFormat();

    /**
     * Get connected {@link IInArchive} for update operations.
     *
     * @return connected {@link IInArchive} for update operations
     */
    public IInArchive getConnectedInArchive();
}

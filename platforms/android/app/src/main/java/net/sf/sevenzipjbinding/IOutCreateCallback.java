package net.sf.sevenzipjbinding;

import net.sf.sevenzipjbinding.impl.OutItemFactory;

/**
 * The interface designed to provide necessary information about new or updated archive items and to receive information
 * about the progress of the operation.
 * 
 * @param <T>
 *            the type of the corresponding archive item data class (out item), like {@link IOutItem7z} or
 *            {@link IOutItemZip}. Use {@link IOutItemAllFormats} interface to support all available archive formats.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutCreateCallback<T extends IOutItemBase> extends IProgress {
    /**
     * Notify about success or failure of the current archive item compression or update operation.
     * 
     * @param operationResultOk
     *            <code>true</code> current archive item was processed successfully, <code>false</code> otherwise.
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public void setOperationResult(boolean operationResultOk) throws SevenZipException;

    /**
     * Get information about archive item with index <code>index</code> being created or updated. Consider following
     * cases:
     * 
     * <ul>
     * <li>New item within a create or an update operation:
     * 
     * <pre>
     * public IOutItemZip getItemInformation(int index, OutItemFactory{@code <}IOutItemZip{@code >} outItemFactory) throws SevenZipException {
     *     IOutItemZip outItem = outItemFactory.createOutItem(); 
     * 
     *     outItem.setDataSize(size);
     *     outItem.setPropertyPath("readme.txt");
     *     
     *     // Set all other required properties here
     * 
     *     return outItem;
     * }
     * </pre>
     * 
     * <li>Item based on an existing item (update only)
     * 
     * <pre>
     * public IOutItemZip getItemInformation(int index, OutItemFactory{@code <}IOutItemZip{@code >} outItemFactory) throws SevenZipException {
     * 
     *     // Determine index of the corresponding existing item in the old archive (archive being updated)
     *     int oldIndex = ...;
     *     
     *     IOutItemZip outItem = outItemFactory.createOutItem(oldIndex); 
     * 
     *     outItem.setPropertyPath("readme.txt");
     *     // Set all other required properties here
     * 
     *     return outItem;
     * }
     * </pre>
     * 
     * <li>Item based on an existing item and inheriting some or all properties (update only)
     * 
     * <pre>
     * public IOutItemZip getItemInformation(int index, OutItemFactory{@code <}IOutItemZip{@code >} outItemFactory) throws SevenZipException {
     * 
     *     // Determine index of the corresponding existing item in the old archive (archive being updated)
     *     int oldIndex = ...;
     * 
     *     IOutItemZip outItem = outItemFactory.createOutItemAndCloneProperties(oldIndex); 
     * 
     *     // Set some properties
     *     outItem.setPropertyAttributes(newAttributes);
     *     
     *     // Keep other properties unchanged
     * 
     *     return outItem;
     * }
     * </pre>
     * 
     * </ul>
     * 
     * @param index
     *            0-based index of the item to get data. Same index returned by {@link IOutItemBase#getIndex()} of the
     *            <code>outItem</code> object.
     * @param outItemFactory
     *            a factory to create a pre-initialized instance of the data object. The created object should be filled
     *            with information about the archive item with index <code>index</code>
     * @return data object created with the <code>outItemFactory</code> filled with all necessary information for the
     *         current operation
     * @throws SevenZipException
     *             in error case. If this method ends with an exception, the current operation will be reported to 7-Zip
     *             as failed. There are no guarantee, that there are no further call back methods will get called. The
     *             first and last thrown exceptions will be saved and thrown later on from the originally called method
     *             such as <code>ISevenZipInArchive.extract()</code> or <code>SevenZip.openInArchive()</code>. Up to
     *             four exceptions depending on the situation can be saved for further analysis. See
     *             {@link SevenZipException} and {@link SevenZipException#printStackTraceExtended()} for details.
     */
    public T getItemInformation(int index, OutItemFactory<T> outItemFactory) throws SevenZipException;

    /**
     * Return sequential in-stream for the archive item with index <code>index</code> to read and compress the content
     * of the item. Depending on a archive format, this method may be called for any archive item including directories.
     * <code>null</code> should be returned for archive items without any content.
     * 
     * @param index
     *            index of the item to read content of (starting from 0)
     * @return sequential in-stream pointed to the content of the archive item with index <code>index</code>. Return
     *         <code>null</code> for archive items without content (for example, for directories)
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public ISequentialInStream getStream(int index) throws SevenZipException;
}

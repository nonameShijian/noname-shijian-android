package net.sf.sevenzipjbinding;

/**
 * The interface provides functionality to update existing 7z archives.<br>
 * The standard way of getting the implementation of this interface is to use
 * {@link IInArchive#getConnectedOutArchive()} method like this:<br>
 * <br>
 *
 * <pre>
 *  {@link IInArchive} inArchive = {@link SevenZip}.openInArchive(null, inStream);
 *  {@link IOutUpdateArchive}{@code <}{@link IOutItemBase}{@code >} outArchive = inArchive.openOutArchive();
 *
 *  if (outArchive instanceof {@link IOutUpdateArchive7z}) {
 *    {@link IOutUpdateArchive7z} outUpdateArchive7z = ({@link IOutUpdateArchive7z})outArchive;
 *    ...
 *  }
 *
 *  outArchive.updateItems(...);
 *
 *  ...
 *
 *  inArchive.close();
 * </pre>
 *
 * @see IOutUpdateArchive
 * @see IInArchive
 * @see IOutItemBase
 * @see IOutItemAllFormats
 *
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutUpdateArchive7z extends IOutUpdateArchive<IOutItem7z>, //
        IOutFeatureSetLevel, //
        IOutFeatureSetSolid, //
        IOutFeatureSetMultithreading, //
        IOutFeatureSetEncryptHeader {
}

package net.sf.sevenzipjbinding;

/**
 * The interface provides functionality to update existing Tar archives.<br>
 * The standard way of getting the implementation of this interface is to use
 * {@link IInArchive#getConnectedOutArchive()} method like this:<br>
 * <br>
 *
 * <pre>
 *  {@link IInArchive} inArchive = {@link SevenZip}.openInArchive(null, inStream);
 *  {@link IOutUpdateArchive}{@code <}{@link IOutItemBase}{@code >} outArchive = inArchive.openOutArchive();
 *
 *  if (outArchive instanceof {@link IOutUpdateArchiveTar}) {
 *    {@link IOutUpdateArchiveTar} outUpdateArchiveTarz = ({@link IOutUpdateArchiveTar})outArchive;
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
public interface IOutUpdateArchiveTar extends IOutUpdateArchive<IOutItemTar> {
}

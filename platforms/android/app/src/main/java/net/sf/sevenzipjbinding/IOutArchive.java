package net.sf.sevenzipjbinding;

/**
 * Interface combining {@link IOutCreateArchive} and {@link IOutUpdateArchive}. Supports creating new archives and
 * updating existing archives. More information see JavaDocs of both interfaces.
 *
 * @see IOutCreateArchive
 * @see IOutUpdateArchive
 *
 * @param <T>
 *            the type of the corresponding archive item data class (out item), like {@link IOutItem7z} or
 *            {@link IOutItemZip}. Use {@link IOutItemAllFormats} interface to support all available archive formats.
 *
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutArchive<T extends IOutItemBase> extends IOutCreateArchive<T>, IOutUpdateArchive<T> {

}

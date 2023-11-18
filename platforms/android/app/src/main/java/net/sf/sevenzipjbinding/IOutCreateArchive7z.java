package net.sf.sevenzipjbinding;

/**
 * The interface provides functionality to create new 7-zip archives.<br>
 * Standard way to get implementation is to use {@link SevenZip#openOutArchive7z()}. See {@link IOutCreateArchive}
 * -JavaDoc for more information.
 *
 * <i>NOTE:</i> Each instance should be closed using {@link IOutArchive#close()} method.
 *
 * @see IOutCreateArchive
 * @see ArchiveFormat#SEVEN_ZIP
 *
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutCreateArchive7z extends IOutCreateArchive<IOutItem7z>, //
        IOutFeatureSetLevel, //
        IOutFeatureSetSolid, //
        IOutFeatureSetMultithreading, //
        IOutFeatureSetEncryptHeader {
}

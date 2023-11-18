package net.sf.sevenzipjbinding.impl;

import net.sf.sevenzipjbinding.IOutCreateArchiveBZip2;
import net.sf.sevenzipjbinding.IOutItemBZip2;
import net.sf.sevenzipjbinding.IOutUpdateArchiveBZip2;

/**
 * BZip2 specific archive create and update class.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public class OutArchiveBZip2Impl extends OutArchiveImpl<IOutItemBZip2> implements IOutCreateArchiveBZip2,
        IOutUpdateArchiveBZip2 {

    /**
     * {@inheritDoc}
     */
    public void setLevel(int compressionLevel) {
        featureSetLevel(compressionLevel);
    }

}

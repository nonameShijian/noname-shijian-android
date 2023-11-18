package net.sf.sevenzipjbinding.impl;

import net.sf.sevenzipjbinding.IOutCreateArchiveGZip;
import net.sf.sevenzipjbinding.IOutItemGZip;
import net.sf.sevenzipjbinding.IOutUpdateArchiveGZip;

/**
 * GZip specific archive create and update class.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public class OutArchiveGZipImpl extends OutArchiveImpl<IOutItemGZip> implements IOutCreateArchiveGZip,
        IOutUpdateArchiveGZip {

    /**
     * {@inheritDoc}
     */
    public void setLevel(int compressionLevel) {
        featureSetLevel(compressionLevel);
    }

}

package net.sf.sevenzipjbinding;

/**
 * Enumeration for possible NCoder properties
 * 
 * TODO Use it or remove it
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public enum NCoderPropID {
    kBlockSize(int.class, "C"), //
    kDictionarySize(int.class, "D"), //
    kUsedMemorySize(int.class, "MEM"), //
    kOrder(int.class, "O"), //
    kPosStateBits(int.class, "PB"), //
    kLitContextBits(int.class, "LC"), //
    kLitPosBits(int.class, "LP"), //
    kEndMarker(boolean.class, "eos"), //
    kNumPasses(int.class, "Pass"), //
    kNumFastBytes(int.class, "fb"), //
    kMatchFinderCycles(int.class, "mc"), //
    kAlgorithm(int.class, "a"), //
    kMatchFinder(String.class, "mf"), //
    kNumThreads(int.class, "mt"), //
    kDefaultProp(int.class, "");

    private final Class<?> clazz;
    private final String option;

    private NCoderPropID(Class<?> clazz, String option) {
        this.clazz = clazz;
        this.option = option;
    }
}

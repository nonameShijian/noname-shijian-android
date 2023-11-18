package net.sf.sevenzipjbinding;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * SevenZip core exception. This exception supports multiple 'cause by' exceptions. Use
 * {@link #printStackTraceExtended()} to get stack traces of all available 'cause by's. Multiple 'cause by' can occur,
 * if native code is involved. If in one of the call-back java methods an exception will be thrown, the native code will
 * save this exception and may proceed with the next call-back java method, which can throw a further exception as well.
 * After native code completes, a new SevenZipException will be thrown. This exception will have multiple 'cause by'
 * exceptions attached:
 * <ul>
 * <li>exception, thrown from the first call-back java method (first 'cause by' exception)
 * <li>exception, thrown from the last call-back java method (last 'cause by' exception)
 * </ul>
 * If more, then two 'cause by' exception was thrown, only first and last exception will be saved.<br>
 * <br>
 * 
 * In case of multi-threaded native code potential 'cause by' exception could be available in a SevenZipException. A
 * potential 'cause by' exception situation can occur, if during a pending native call, a call-back java method will be
 * called from a new thread. Since an exception thrown in another thread can't be always reliably connected to the
 * operation proceeding (main native call), the exception will be saves as a potential first/last cause.<br>
 * <br>
 * The methods
 * <ul>
 * <li>{@link #printStackTraceExtended()}
 * <li>{@link #printStackTraceExtended(PrintStream)}
 * <li>{@link #printStackTraceExtended(PrintWriter)}
 * </ul>
 * provide full information about underlying 'cause by' exceptions and nested {@link SevenZipException}. In order to
 * improve readability of the long extended stack traces, the origin of the current exception being printed can be read
 * vertically on the left of the stack trace. The standard {@link #printStackTrace()} method prints only the stack trace
 * of the first 'cause by' exception.
 * 
 * @see #printStackTraceExtended()
 * @see #printStackTraceExtended(PrintStream)
 * @see #printStackTraceExtended(PrintWriter)
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public class SevenZipException extends IOException {
    private static final String NEW_LINE = System.getProperty("line.separator");

    private static class StackTraceWriter extends Writer {
        private PrintWriter writer;
        private boolean newlineFound = true;
        private final String message;
        private int indexInMessage;

        StackTraceWriter(PrintWriter writer, String message) {
            super(writer);
            this.writer = writer;
            this.message = message;
        }

        @Override
        public void close() {
            writer.close();
        }

        @Override
        public void flush() {
            writer.flush();
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            for (int i = 0; i < len; i++) {
                if (cbuf[i] == '\n' || cbuf[i] == '\r') {
                    newlineFound = true;
                } else {
                    if (newlineFound) {
                        if (message.charAt(indexInMessage) == ' ') {
                            writer.write("| ");
                        } else {
                            writer.write(message.charAt(indexInMessage));
                            writer.write(' ');
                        }
                        if (++indexInMessage >= message.length()) {
                            indexInMessage = 0;
                        }
                    }
                    newlineFound = false;
                }
                writer.write(cbuf[i]);
            }
        }
    }

    private static class PrintStreamWriter extends Writer {
        private final PrintStream printStream;

        PrintStreamWriter(PrintStream printStream) {
            this.printStream = printStream;
        }

        @Override
        public void close() throws IOException {
            printStream.close();
        }

        @Override
        public void flush() throws IOException {
            printStream.flush();
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (off == 0 && len == cbuf.length) {
                printStream.print(cbuf);
            } else {
                printStream.print(new String(cbuf, off, len));
            }
        }
    }

    private static final long serialVersionUID = 42L;

    private Throwable causeLastThrown;
    private Throwable causeFirstPotentialThrown;
    private Throwable causeLastPotentialThrown;

    /**
     * Constructs a new exception with <code>null</code> as its detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable)}.
     */
    public SevenZipException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this
     * exception's detail message.
     * 
     * @param message
     *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @since 1.4
     */
    public SevenZipException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause(Throwable)}.
     * 
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *            method.
     */
    public SevenZipException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
     * <tt>cause</tt>). This constructor is useful for exceptions that are little more than wrappers for other
     * throwables (for example, <code>PrivilegedActionException</code>).
     * 
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @since 1.4
     */
    public SevenZipException(Throwable cause) {
        super();
        initCause(cause);
    }

    /**
     * This setter will be used by native code
     */
    @SuppressWarnings("unused")
    private void setCauseLastThrown(Throwable causeLastThrown) {
        this.causeLastThrown = causeLastThrown;
    }

    /**
     * This setter will be used by native code
     */
    @SuppressWarnings("unused")
    private void setCauseFirstPotentialThrown(Throwable causeFirstPotentialThrown) {
        this.causeFirstPotentialThrown = causeFirstPotentialThrown;
    }

    /**
     * This setter will be used by native code
     */
    @SuppressWarnings("unused")
    private void setCauseLastPotentialThrown(Throwable causeLastPotentialThrown) {
        this.causeLastPotentialThrown = causeLastPotentialThrown;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return getMessage(false);
    }

    /**
     * Get 7-Zip-JBinding exception original error message (without 'cause by' messages)
     * 
     * @return original error message
     */
    public String getSevenZipExceptionMessage() {
        return super.getMessage();
    }

    private String getMessage(boolean forPrintStackTraceExtended) {
        if (causeLastThrown == null && causeFirstPotentialThrown == null && causeLastPotentialThrown == null) {
            return super.getMessage();
        }
        StringBuilder stringBuilder = new StringBuilder();
        printToStringBuilder("", stringBuilder, forPrintStackTraceExtended);
        return stringBuilder.toString();
    }

    private void printToStringBuilder(String prefix, StringBuilder stringBuilder, boolean forPrintStackTraceExtended) {
        String message = super.getMessage();
        if (message == null) {
            stringBuilder.append("No message");
        } else {
            stringBuilder.append(message);
        }

        if (prefix.length() == 0 && !forPrintStackTraceExtended) {
            stringBuilder.append(NEW_LINE);
            stringBuilder.append("This " + SevenZipException.class.getSimpleName()
                    + " has multiple 'cause by' exceptions. Use one of the " + SevenZipException.class.getSimpleName()
                    + ".printStackTraceExtended(..) methods "
                    + "to get stack trace of last thrown and first/last potiential thrown 'cause by' exceptions.");
        }

        Throwable causeFirstThrown = getCause();

        if (causeFirstThrown != null) {
            stringBuilder.append(NEW_LINE);
            stringBuilder.append(prefix);
            stringBuilder.append("Caused by (first thrown): ");
            printMessageToStringBuilder(prefix, stringBuilder, causeFirstThrown);
        }

        if (causeLastThrown != null) {
            stringBuilder.append(NEW_LINE);
            stringBuilder.append(prefix);
            stringBuilder.append("Caused by (last thrown): ");
            printMessageToStringBuilder(prefix, stringBuilder, causeLastThrown);
        }

        if (causeFirstPotentialThrown != null) {
            stringBuilder.append(NEW_LINE);
            stringBuilder.append(prefix);
            stringBuilder.append("Caused by (first potential thrown): ");
            printMessageToStringBuilder(prefix, stringBuilder, causeFirstPotentialThrown);
        }

        if (causeLastPotentialThrown != null) {
            stringBuilder.append(NEW_LINE);
            stringBuilder.append(prefix);
            stringBuilder.append("Caused by (last potential thrown): ");
            printMessageToStringBuilder(prefix, stringBuilder, causeLastPotentialThrown);
        }
    }

    private void printMessageToStringBuilder(String prefix, StringBuilder stringBuilder, Throwable cause) {
        stringBuilder.append(cause.getClass().getCanonicalName());
        stringBuilder.append(": ");
        if (cause instanceof SevenZipException) {
            ((SevenZipException) cause).printToStringBuilder(prefix + "  ", stringBuilder, false);
        } else {
            stringBuilder.append(cause.getMessage());
        }
    }

    /**
     * Prints stack traces of this SevenZipException and of the all thrown 'cause by' exceptions to the specified system
     * error stream <code>System.err</code>.
     */
    public void printStackTraceExtended() {
        printStackTraceExtended(System.err);
    }

    /**
     * Prints stack trace of this SevenZipException and of the all thrown 'cause by' exceptions to the specified print
     * stream.
     * 
     * @param printStream
     *            <code>PrintStream</code> to use for output
     */
    public void printStackTraceExtended(PrintStream printStream) {
        synchronized (printStream) {
            printStackTraceToPrintWriter(new PrintWriter(new PrintStreamWriter(printStream)), this, null);
        }
    }

    /**
     * Prints stack trace of this SevenZipException and of the all thrown 'cause by' exceptions to the specified print
     * writer.
     * 
     * @param printWriter
     *            <code>PrintWriter</code> to use for output
     */
    public void printStackTraceExtended(PrintWriter printWriter) {
        synchronized (printWriter) {
            printStackTraceToPrintWriter(printWriter, this, null);
        }
    }

    private static void printStackTraceOfThrowable(PrintWriter printWriter, Throwable throwable, Throwable successor) {
        if (throwable instanceof SevenZipException) {
            printStackTraceToPrintWriter(printWriter, (SevenZipException) throwable, successor);
        } else {
            printStackTraceRecursive(printWriter, throwable, successor);
        }
    }

    private static void printStackTraceToPrintWriter(PrintWriter printWriter, SevenZipException sevenZipException,
            Throwable successor) {
        if (sevenZipException.causeLastThrown == null && sevenZipException.causeFirstPotentialThrown == null
                && sevenZipException.causeLastPotentialThrown == null) {
            printStackTraceRecursive(printWriter, sevenZipException, successor);
        }

        printWriter.println("+------ " + SevenZipException.class.getSimpleName()
                + " with multiple 'cause by' exceptions. Stacktraces for all involved exceptions:");
        printWriter.println("+-- The " + SevenZipException.class.getSimpleName()
                + " itself with first thrown 'cause by' exception (first cause): ");
        //        if (successor == null) {
        //        } else {
        //            printWriter.println("+-- First thrown 'cause by' exception (first cause): ");
        //        }
        printStackTraceRecursive(new PrintWriter(new StackTraceWriter(printWriter, "  FIRST THROWN CAUSE ")),
                sevenZipException, successor);
        if (sevenZipException.causeLastThrown != null) {
            printWriter.println("+-- Last thrown 'cause by' exception (last cause): ");
            printStackTraceOfThrowable(new PrintWriter(new StackTraceWriter(printWriter, "  LAST THROWN CAUSE ")),
                    sevenZipException.causeLastThrown, sevenZipException);
        }
        if (sevenZipException.causeFirstPotentialThrown != null) {
            printWriter.println(//
                    "+-- First thrown potential 'cause by' exception, thrown in an other thread (first possible cause): ");
            printStackTraceOfThrowable(new PrintWriter(new StackTraceWriter(printWriter,
                    "  FIRST THROWN POTENTIAL CAUSE ")), sevenZipException.causeFirstPotentialThrown, sevenZipException);
        }
        if (sevenZipException.causeLastPotentialThrown != null) {
            printWriter.println(//
                    "+-- Last thrown potential 'cause by' exception, thrown in an other thread (last possible cause): ");
            printStackTraceOfThrowable(new PrintWriter(new StackTraceWriter(printWriter,
                    "  LAST THROWN POTENTIAL CAUSE ")), sevenZipException.causeLastPotentialThrown, sevenZipException);
        }
        printWriter.println("+------ End of stacktrace of " + SevenZipException.class.getSimpleName()
                + " with multiple 'cause by' exceptions");

    }

    private static void printStackTraceRecursive(PrintWriter printWriter, Throwable throwable, Throwable successor) {
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        int skipElements = 0;
        if (successor != null) {
            StackTraceElement[] successorStackTraceElements = successor.getStackTrace();
            int i = successorStackTraceElements.length - 1;
            int j = stackTraceElements.length - 1;
            while (i >= 3 && j >= 3 && successorStackTraceElements[i].equals(stackTraceElements[j])) {
                i--;
                j--;
                skipElements++;
            }
        }
        if (skipElements <= 3) {
            skipElements = 0;
        }
        if (successor == null) {
            printWriter.println(getMessageForPrintStackTraceExtended(throwable));
        } else {
            printWriter.println("Caused by: " + getMessageForPrintStackTraceExtended(throwable));
        }
        for (int i = 0; i < stackTraceElements.length - skipElements; i++) {
            StackTraceElement stackTraceElement = stackTraceElements[i];
            printWriter.println("\tat " + stackTraceElement);
        }
        if (skipElements > 0) {
            printWriter.println("\t... " + skipElements + " more");
        }
        if (throwable.getCause() != null) {
            printStackTraceOfThrowable(printWriter, throwable.getCause(), throwable);
        }
    }

    private static String getMessageForPrintStackTraceExtended(Throwable throwable) {
        if (throwable instanceof SevenZipException) {
            return ((SevenZipException) throwable).getMessage(true);
        } else {
            return throwable.toString();
        }
    }

    /**
     * Get first thrown exception as a cause for this exception.
     */
    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    /**
     * Get last thrown exception as a cause for this exception.
     * 
     * @return <code>null</code> if no other (different to {@link #getCause()}) exception known as a cause for this
     *         exception.
     */
    public Throwable getCauseLastThrown() {
        return causeLastThrown;
    }

    /**
     * Get the last thrown exception that was the potential cause for this exception. The potential cause is an
     * exception thrown in another thread where it's not possible to determine whether the exception has caused the
     * current exception or not.
     * 
     * @return <code>null</code> if no such exception was thrown.
     */
    public Throwable getCauseLastPotentialThrown() {
        return causeLastPotentialThrown;
    }

    /**
     * Get the first thrown exception that was the potential cause for this exception. The potential cause is an
     * exception thrown in another thread where it's not possible to determine whether the exception has caused the
     * current exception or not.
     * 
     * @return <code>null</code> if no such exception was thrown.
     */
    public Throwable getCauseFirstPotentialThrown() {
        return causeFirstPotentialThrown;
    }

}

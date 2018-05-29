package priv.marionette.compress;

import priv.marionette.tools.Constants;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 压缩输入流
 */
public class LZFOutputStream extends OutputStream {

    /**
     * Magic Header
     */
    static final int MAGIC = ('M' << 24) | ('A' << 16) | ('R' << 8) | 'I';

    private final OutputStream out;
    private final CompressLZF compress = new CompressLZF();
    private final byte[] buffer;
    private int pos;
    private byte[] outBuffer;

    public LZFOutputStream(OutputStream out) throws IOException {
        this.out = out;
        int len = Constants.IO_BUFFER_SIZE_COMPRESS;
        buffer = new byte[len];
        ensureOutput(len);
        writeInt(MAGIC);
    }

    private void ensureOutput(int len) {
        // buffer
        int outputLen = (len < 100 ? len + 100 : len) * 2;
        if (outBuffer == null || outBuffer.length < outputLen) {
            outBuffer = new byte[outputLen];
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (pos >= buffer.length) {
            flush();
        }
        buffer[pos++] = (byte) b;
    }

    private void compressAndWrite(byte[] buff, int len) throws IOException {
        if (len > 0) {
            ensureOutput(len);
            int compressed = compress.compress(buff, len, outBuffer, 0);
            if (compressed > len) {
                writeInt(-len);
                out.write(buff, 0, len);
            } else {
                writeInt(compressed);
                writeInt(len);
                out.write(outBuffer, 0, compressed);
            }
        }
    }

    private void writeInt(int x) throws IOException {
        out.write((byte) (x >> 24));
        out.write((byte) (x >> 16));
        out.write((byte) (x >> 8));
        out.write((byte) x);
    }

    @Override
    public void write(byte[] buff, int off, int len) throws IOException {
        while (len > 0) {
            int copy = Math.min(buffer.length - pos, len);
            System.arraycopy(buff, off, buffer, pos, copy);
            pos += copy;
            if (pos >= buffer.length) {
                flush();
            }
            off += copy;
            len -= copy;
        }
    }

    @Override
    public void flush() throws IOException {
        compressAndWrite(buffer, pos);
        pos = 0;
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

}

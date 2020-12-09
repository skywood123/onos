package sun.net.httpserver;

import java.io.IOException;
import java.io.OutputStream;

class PlaceholderOutputStream extends OutputStream {
    OutputStream wrapped;

    PlaceholderOutputStream(OutputStream paramOutputStream) {
        this.wrapped = paramOutputStream;
    }

    void setWrappedStream(OutputStream paramOutputStream) {
        this.wrapped = paramOutputStream;
    }

    boolean isWrapped() {
        return (this.wrapped != null);
    }

    private void checkWrap() throws IOException {
        if (this.wrapped == null)
            throw new IOException("response headers not sent yet");
    }

    public void write(int paramInt) throws IOException {
        checkWrap();
        this.wrapped.write(paramInt);
    }

    public void write(byte[] paramArrayOfbyte) throws IOException {
        checkWrap();
        this.wrapped.write(paramArrayOfbyte);
    }

    public void write(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) throws IOException {
        checkWrap();
        this.wrapped.write(paramArrayOfbyte, paramInt1, paramInt2);
    }

    public void flush() throws IOException {
        checkWrap();
        this.wrapped.flush();
    }

    public void close() throws IOException {
        checkWrap();
        this.wrapped.close();
    }
}

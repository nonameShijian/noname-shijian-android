/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.jetty.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.WebSocket.FrameConnection;
import org.eclipse.jetty.websocket.WebSocketParser.FrameHandler;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;

/**
 * PerMessageDeflateExtension
 *
 * Copyright (c) 2015 KNOWLEDGECODE
 */
public class PerMessageDeflateExtension implements Extension {

    private static final Logger __log = Log.getLogger(PerMessageDeflateExtension.class.getName());
    private static final byte[] TAIL_BYTES = new byte[] { 0x00, 0x00, (byte) 0xff, (byte) 0xff };
    private static final int TAIL_LENGTH = TAIL_BYTES.length;
    private static final int INITIAL_CAPACITY = 16384;
    private static final String EXTENSION = "permessage-deflate";
    private static final String CLIENT_NO_CONTEXT_TAKEOVER = "client_no_context_takeover";
    private static final String SERVER_NO_CONTEXT_TAKEOVER = "server_no_context_takeover";
    private static final String SERVER_MAX_WINDOW_BITS = "server_max_window_bits";

    private static class Zlib {
        protected Deflater _deflater;
        protected Inflater _inflater;
        protected byte[] _buffer;
        protected int _capcacity;
        protected boolean _deflaterReset;
        protected boolean _inflaterReset;

        public Zlib(boolean deflaterReset, boolean inflaterReset) {
            _deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            _inflater = new Inflater(true);
            _buffer = new byte[INITIAL_CAPACITY];
            _capcacity = _buffer.length;
            _deflaterReset = deflaterReset;
            _inflaterReset = inflaterReset;
        }

        public static byte[] appendTailBytes(byte[] array, int offset, int length) {
            byte[] buffer = Arrays.copyOfRange(array, offset, offset + length + TAIL_LENGTH);
            System.arraycopy(TAIL_BYTES, 0, buffer, length, TAIL_LENGTH);
            return buffer;
        }

        public byte[] compress(byte[] b, int offset, int length) {
            int len;
            int position = 0;

            _deflater.reset();
            _deflater.setInput(b, offset, length);
            _deflater.finish();
            while ((len = _deflater.deflate(_buffer, position, _capcacity - position)) > 0) {
                if ((position += len) == _capcacity) {
                    _buffer = Arrays.copyOf(_buffer, _capcacity <<= 1);
                }
            }
            return Arrays.copyOf(_buffer, position);
        }

        public byte[] decompress(byte[] b) throws DataFormatException {
            return decompress(b, 0, b.length);
        }

        public byte[] decompress(byte[] b, int offset, int length) throws DataFormatException {
            int len;
            int position = 0;

            if (_inflaterReset) {
                _inflater.reset();
            }
            _inflater.setInput(b, offset, length);
            while ((len = _inflater.inflate(_buffer, position, _capcacity - position)) > 0) {
                if ((position += len) == _capcacity) {
                    _buffer = Arrays.copyOf(_buffer, _capcacity <<= 1);
                }
            }
            return Arrays.copyOf(_buffer, position);
        }

        public boolean isCompressible() {
            return _deflaterReset;
        }

        public void end() {
            _deflater.end();
            _inflater.end();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static class NewZlib extends Zlib {
        public NewZlib(boolean deflaterReset, boolean inflaterReset) {
            super(deflaterReset, inflaterReset);
        }

        @Override
        public byte[] compress(byte[] b, int offset, int length) {
            int len;
            int position = 0;

            if (_deflaterReset) {
                _deflater.reset();
            }
            _deflater.setInput(b, offset, length);
            while ((len = _deflater.deflate(_buffer, position, _capcacity - position, Deflater.SYNC_FLUSH)) > 0) {
                if ((position += len) == _capcacity) {
                    _buffer = Arrays.copyOf(_buffer, _capcacity <<= 1);
                }
            }
            return Arrays.copyOf(_buffer, position - TAIL_LENGTH);
        }

        @Override
        public boolean isCompressible() {
            return true;
        }
    }

    private FrameConnection _connection;
    private FrameHandler _inbound;
    private WebSocketGenerator _outbound;
    private Zlib _zlib;
    private String _parameters;
    private boolean _compressed;

    public PerMessageDeflateExtension() {
        if (Build.VERSION.SDK_INT < 19) {
            _parameters = TextUtils.join("; ", new String[]{ EXTENSION, CLIENT_NO_CONTEXT_TAKEOVER });
        } else {
            _parameters = EXTENSION;
        }
        _compressed = false;
    }

    @Override
    public void onFrame(byte flags, byte opcode, byte[] array, int offset, int length) {
        switch (opcode) {
        case WebSocketConnectionRFC6455.OP_TEXT:
        case WebSocketConnectionRFC6455.OP_BINARY:
            _compressed = ((flags & 0x07) == 0x04);
        case WebSocketConnectionRFC6455.OP_CONTINUATION:
            if (_compressed) {
                try {
                    byte[] buffer;
                    if ((flags &= 0x08) > 0) {
                        buffer = _zlib.decompress(Zlib.appendTailBytes(array, offset, length));
                    } else {
                        buffer = _zlib.decompress(array, offset, length);
                    }
                    _inbound.onFrame(flags, opcode, buffer, 0, buffer.length);
                } catch (DataFormatException e) {
                    __log.warn(e);
                    _connection.close(WebSocketConnectionRFC6455.CLOSE_BAD_DATA, "Bad data");
                }
                return;
            }
        }
        _inbound.onFrame(flags, opcode, array, offset, length);
    }

    @Override
    public void close(int code, String message) {
        _zlib.end();
    }

    @Override
    public int flush() throws IOException {
        return 0;
    }

    @Override
    public boolean isBufferEmpty() {
        return _outbound.isBufferEmpty();
    }

    @Override
    public void addFrame(byte flags, byte opcode, byte[] content, int offset, int length) throws IOException {
        if (opcode == WebSocketConnectionRFC6455.OP_TEXT || opcode == WebSocketConnectionRFC6455.OP_BINARY) {
            if (_zlib.isCompressible()) {
                byte[] compressed = _zlib.compress(content, offset, length);
                _outbound.addFrame((byte) (flags | 0x04), opcode, compressed, 0, compressed.length);
                return;
            }
        }
        _outbound.addFrame(flags, opcode, content, offset, length);
    }

    @Override
    public String getName() {
        return EXTENSION;
    }

    @Override
    public String getParameterizedName() {
        return _parameters;
    }

    @Override
    public boolean init(Map<String, String> parameters) {
        boolean extension = false;
        boolean client_no_context_takeover = false;
        boolean server_no_context_takeover = false;
        int server_max_window_bits = 0;

        _parameters = "";
        for (String key : parameters.keySet()) {
            String value = parameters.get(key);

            if (EXTENSION.equals(key) && TextUtils.isEmpty(value) && !extension) {
                extension = true;
            } else if (CLIENT_NO_CONTEXT_TAKEOVER.equals(key) && TextUtils.isEmpty(value) && !client_no_context_takeover) {
                client_no_context_takeover = true;
            } else if (SERVER_NO_CONTEXT_TAKEOVER.equals(key) && TextUtils.isEmpty(value) && !server_no_context_takeover) {
                server_no_context_takeover = true;
            } else if (SERVER_MAX_WINDOW_BITS.equals(key) && server_max_window_bits == 0) {
                if (TextUtils.isEmpty(value)) {
                    __log.warn("Unexpected parameter: {}", key);
                    return false;
                }
                if (!value.matches("[1-9]\\d?")) {
                    __log.warn("Unexpected parameter: {}={}", key, value);
                    return false;
                }
                int v = Integer.parseInt(value);
                if (v < 8 || v > 15) {
                    __log.warn("Unexpected parameter: {}={}", key, value);
                    return false;
                }
                server_max_window_bits = v;
            } else if (!TextUtils.isEmpty(value)) {
                __log.warn("Unexpected parameter: {}={}", key, value);
                return false;
            } else {
                __log.warn("Unexpected parameter: {}", key);
                return false;
            }
        }
        if (extension) {
            List<String> p = new ArrayList<String>();

            p.add(EXTENSION);
            if (client_no_context_takeover) {
                p.add(CLIENT_NO_CONTEXT_TAKEOVER);
            }
            if (server_no_context_takeover) {
                p.add(SERVER_NO_CONTEXT_TAKEOVER);
            }
            if (server_max_window_bits > 0) {
                p.add(String.format("%s=%d", SERVER_MAX_WINDOW_BITS, server_max_window_bits));
            }
            _parameters = TextUtils.join("; ", p);

            if (Build.VERSION.SDK_INT < 19) {
                _zlib = new Zlib(client_no_context_takeover, server_no_context_takeover);
            } else {
                _zlib = new NewZlib(client_no_context_takeover, server_no_context_takeover);
            }
        }
        return extension;
    }

    @Override
    public void bind(FrameConnection connection, FrameHandler inbound, WebSocketGenerator outbound) {
        _connection = connection;
        _inbound = inbound;
        _outbound = outbound;
    }
}

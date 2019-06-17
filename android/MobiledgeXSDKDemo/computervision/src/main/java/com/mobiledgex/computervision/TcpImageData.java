package com.mobiledgex.computervision;

import com.xuhao.didi.core.iocore.interfaces.ISendable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TcpImageData implements ISendable {
    private String str = "";
    private int mOpcode = 0;
    private byte[] mPayload;

    public TcpImageData(int opcode, byte[] payload) {
        mOpcode = opcode;
        mPayload = payload;
    }

    @Override
    public byte[] parse() {
        //Build the byte array according to the server's parsing rules
        //package header fixed length + opcode length + payload length
        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + mPayload.length);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(mOpcode);
        bb.putInt(mPayload.length);
        bb.put(mPayload);
        return bb.array();
    }
}


package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_PACKET;

public class TCP_PACKET_WITH_CHECK extends TCP_PACKET {
    public TCP_PACKET_WITH_CHECK(){
        super();
        this.acked = false;
    }
    public boolean acked = false;
}

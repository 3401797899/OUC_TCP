package com.ouc.tcp.test;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.TimerTask;

public class TCP_PACKET_WITH_CHECK {
    public boolean acked; // 当前包是否被ack
    private TCP_Sender sender; // 保存发送者的引用，用于发送数据
    private UDT_Timer timer; // 超时定时器
    private TCP_PACKET packet; // 包

    public TCP_PACKET_WITH_CHECK(TCP_Sender sender, TCP_PACKET packet) {
        super();
        this.acked = false;
        this.sender = sender;
        this.packet = packet;
        this.timer = new UDT_Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!acked){
                    sender.udt_send(packet);
                }
            }
        }, 0, 100);
    }

    public void ack() {
        this.acked = true;
        this.timer.cancel();
    }

    public int get_seq() {
        return packet.getTcpH().getTh_seq();
    }
}

package com.ouc.tcp.test;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;


public class SlideWindow {
    int size = 4; // 窗口大小
    private final LinkedBlockingQueue<TCP_PACKET_WITH_CHECK> window = new LinkedBlockingQueue<>(this.size); // 窗口
    TCP_Sender sender; // 保存发送者的引用，用于发送数据


    public SlideWindow(TCP_Sender sender) {
        this.sender = sender;
    }

    public boolean isFull() {
        return window.size() == size;
    }

    public void add(TCP_PACKET packet) {
        window.add((new TCP_PACKET_WITH_CHECK(sender, packet)));
    }

    public void recv(TCP_PACKET packet) {
        for (TCP_PACKET_WITH_CHECK p : window) {
            // 对包进行ack
            if (!p.acked && p.get_seq() == packet.getTcpH().getTh_ack()) {
                p.ack();
            }
        }
        // 滑动窗口
        while(!window.isEmpty() && window.peek().acked){
            window.poll();
        }
    }
}

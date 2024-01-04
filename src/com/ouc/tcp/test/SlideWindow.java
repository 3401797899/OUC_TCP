package com.ouc.tcp.test;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;


public class SlideWindow {
    int size = 5; // 窗口大小
    private final LinkedBlockingQueue<TCP_PACKET> window = new LinkedBlockingQueue<>(this.size); // 窗口
    private UDT_Timer timer; // 超时计时器
    TCP_Sender sender; // 保存发送者的引用，用于发送数据


    public SlideWindow(TCP_Sender sender) {
        this.sender = sender;
    }

    private void start_timer() {
        timer = new UDT_Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                send();
            }
        }, 100, 100);
    }

    public boolean isFull() {
        return window.size() == size;
    }

    public void add(TCP_PACKET packet) {
        window.add(packet);
        // 取消之前的计时器
        if (timer != null) {
            timer.cancel();
        }
        // 重启计时器
        start_timer();
    }

    public void udt_send(TCP_PACKET stcpPack) {
        //发送数据报
        sender.client.send(stcpPack);
    }


    public void send() {
        for (TCP_PACKET packet : window) {
            udt_send(packet);
        }
    }

    public void recv(TCP_PACKET packet) {
        for (TCP_PACKET p : window) {
            // 对累积确认的处理
            if (p.getTcpH().getTh_seq() <= packet.getTcpH().getTh_ack()) {
                window.poll();
            } else {
                break;
            }
        }
        if(window.isEmpty()) {
            timer.cancel();
        }
    }
}

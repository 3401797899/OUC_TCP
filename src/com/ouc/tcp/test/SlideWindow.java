package com.ouc.tcp.test;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;


public class SlideWindow {
    int size = 8; // 窗口大小
    private final LinkedBlockingQueue<TCP_PACKET> window = new LinkedBlockingQueue<>(this.size); // 窗口
    private UDT_Timer timer; // 超时计时器
    TCP_Sender sender; // 保存发送者的引用，用于发送数据
    int last_acked = -1;
    int ack_count = 1;
    int resend_threshold = 3;
    TCP_PACKET last_packet;


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


    public void send() {
        for (TCP_PACKET packet : window) {
            sender.udt_send(packet);
        }
    }

    public void resend() {
        for (TCP_PACKET packet : window) {
            if (packet.getTcpH().getTh_seq() == last_acked + 100) {
//                System.out.println("快重传" + packet.getTcpH().getTh_seq() + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                sender.udt_send(packet);
            } else if (packet.getTcpH().getTh_seq() > last_acked + 100) {
                break;
            }
        }
    }

    public void recv(TCP_PACKET packet) {
        if (packet.getTcpH().getTh_ack() == last_acked) {
            // 累积数量到达阈值，立即重发
            if (ack_count == resend_threshold) {
                ack_count = 0;
                resend();
                timer.cancel();
                start_timer();
            }
            // 数量 + 1
            ack_count++;
        } else {
            last_acked = packet.getTcpH().getTh_ack();
            ack_count = 1;
        }

        for(TCP_PACKET p : window){
            if(p.getTcpH().getTh_seq() <= packet.getTcpH().getTh_ack()){
                window.poll();
            }
        }

        if (window.isEmpty()) {
            timer.cancel();
        }
    }
}
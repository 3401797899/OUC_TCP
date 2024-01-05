package com.ouc.tcp.test;

import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;


public class SlideWindow {
    int cwnd = 1; // 窗口大小
    int ssThresh = 16; // 慢开始阈值
    int ca_next = -1; // 拥塞避免阶段下一次增大窗口的ack包的值

    private final LinkedBlockingQueue<TCP_PACKET> window = new LinkedBlockingQueue<>(); // 窗口
    private UDT_Timer timer; // 超时计时器
    TCP_Sender sender; // 保存发送者的引用，用于发送数据
    int last_acked = -1;
    int ack_count = 1;
    int resend_threshold = 3;

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
        return window.size() == cwnd;
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
        int i = 0;

        for (TCP_PACKET packet : window) {
            if (i == cwnd) {
                break;
            }
            sender.udt_send(packet);
            i++;
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
        System.out.println("当前拥塞窗口大小为：" + cwnd);
        if (cwnd < ssThresh) {
            // 慢开始算法
            cwnd++;
            System.out.println("慢开始算法， 窗口大小+1");
        } else {
            if(ca_next == -1){
                ca_next = packet.getTcpH().getTh_ack();
            }
            // 拥塞避免
            if(packet.getTcpH().getTh_ack() >= ca_next){ // 不用等于，因为对应的那个ack包可能丢了
                System.out.println("拥塞避免算法， 窗口大小+1");
                ca_next = ca_next + cwnd * 100;
                cwnd++;
            }
        }

        if (packet.getTcpH().getTh_ack() == last_acked) {
            // 快重传
            // 累积数量到达阈值，立即重发
            if (ack_count == resend_threshold) {
                // 拥塞避免
                ssThresh = cwnd / 2;
                cwnd = ssThresh + 3;

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

        for (TCP_PACKET p : window) {
            if (p.getTcpH().getTh_seq() <= packet.getTcpH().getTh_ack()) {
                window.poll();
            }
        }

        if (window.isEmpty()) {
            timer.cancel();
        }
    }
}
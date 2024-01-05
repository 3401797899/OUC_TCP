package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;

import com.ouc.tcp.message.TCP_SEGMENT;

public class ReceiverSlideWindow {
    int start = 0; // 窗口起始序号
//    int size = 8; // 窗口大小 (假设无限大，以保证能正常测试拥塞控制)
    private final ArrayList<TCP_PACKET> window = new ArrayList<>(); // 窗口
    private TCP_Receiver receiver; // 保存发送者的引用
    protected TCP_HEADER tcpH; // 保存TCP报文头的引用，用于构建ACK包
    protected TCP_SEGMENT tcpS; // 保存TCP报文段的引用，用于构建ACK包
    protected Queue<int[]> dataQueue; // 保存对接受端接收数据的队列的引用，用于交付数据


    public ReceiverSlideWindow(TCP_Receiver receiver, TCP_HEADER tcpH, Queue<int[]> dataQueue, TCP_SEGMENT tcpS) {
        this.receiver = receiver;
        this.tcpH = tcpH;
        this.dataQueue = dataQueue;
        this.tcpS = tcpS;
    }


    public void recv(TCP_PACKET recvPack) {
//        int end = start + size - 1;
        int seq = (recvPack.getTcpH().getTh_seq() - 1) / 100;
        int index = seq - start;
        if(seq >= start) {
            if(window.size() <= index + 1){
                // set null
                for(int i = window.size(); i <= index; i++){
                    window.add(null);
                }
            }
            window.set(index, recvPack);
        }

        // 数据交到缓存区
        int i = 0;
        for(; i < window.size() && window.get(i) != null; ++i){
            dataQueue.add(recvPack.getTcpS().getData());
//            System.out.println("交付数据" + (start + i) + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            start++;
        }
        TCP_PACKET ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        // 由于程序中ACK是对当前包的确认，所以需要 -1
        tcpH.setTh_ack((start - 1) * 100 + 1);
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        receiver.reply(ackPack);

        // 滑动窗口
        for(int j = 0;j < window.size() - i; j++){
            window.set(j, window.get(j + i));
        }
        while (i > 0) {
            window.set(window.size() - i, null);
            i--;
        }

        // 交付数据
        if (dataQueue.size() == 20)
            receiver.deliver_data();
    }
}

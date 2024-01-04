package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_SEGMENT;

public class ReceiverSlideWindow {
    int start = 0; // 窗口起始序号
    int size = 4; // 窗口大小
    private final ArrayList<TCP_PACKET> window = new ArrayList<>(Collections.nCopies(size, null)); // 窗口
    private TCP_Receiver receiver; // 保存发送者的引用
    protected TCP_HEADER tcpH;
    protected TCP_SEGMENT tcpS;
    protected Queue<int[]> dataQueue;


    public ReceiverSlideWindow(TCP_Receiver receiver, TCP_HEADER tcpH, Queue<int[]> dataQueue, TCP_SEGMENT tcpS) {
        this.receiver = receiver;
        this.tcpH = tcpH;
        this.dataQueue = dataQueue;
        this.tcpS = tcpS;
    }


    public void recv(TCP_PACKET recvPack) {
        int end = start + size - 1;
        int seq = (recvPack.getTcpH().getTh_seq() - 1) / 100;
        int index = seq - start;
        if(seq <= end) {
            // 发送ack
            tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
            TCP_PACKET ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
            tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
            receiver.reply(ackPack);
            if(seq >= start){
                window.set(index, recvPack);
            }
        }
        // 数据交到缓存区
        int i = 0;
        for(; i < window.size() && window.get(i) != null; ++i){
            dataQueue.add(recvPack.getTcpS().getData());
            start++;
        }
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

package com.ecnu.pagerank.giraph;

import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import java.io.IOException;

/**
 * Giraph PageRank 实现
 * * 泛型参数说明:
 * <LongWritable>   : 顶点 ID (Vertex ID)
 * <DoubleWritable> : 顶点值 (Vertex Value - 即 PR 值)
 * <FloatWritable>  : 边数据 (Edge Data - 边权重，虽然 PageRank 通常不需要权重，但格式要求占位)
 * <DoubleWritable> : 消息数据 (Message Data - 传递的 PR 分量)
 */
public class PageRankComputation extends BasicComputation<LongWritable, DoubleWritable, FloatWritable, DoubleWritable> {

    // 阻尼系数
    public static final double DAMPING_FACTOR = 0.85;
    // 最大迭代次数
    public static final int MAX_SUPERSTEPS = 10;

    @Override
    public void compute(Vertex<LongWritable, DoubleWritable, FloatWritable> vertex,
                        Iterable<DoubleWritable> messages) throws IOException {

        // --- 第 0 轮 (Superstep 0): 初始化 ---
        if (getSuperstep() == 0) {
            // 初始 PR 值设为 1.0
            vertex.setValue(new DoubleWritable(1.0));
        } else {
            // --- 后续轮次: 计算新 PR 值 ---
            double sumMessages = 0.0;
            // 1. 累加收到的所有消息
            for (DoubleWritable message : messages) {
                sumMessages += message.get();
            }

            // 2. 应用 PageRank 公式: PR = (1-d) + d * sum(incoming)
            double newPr = (1.0 - DAMPING_FACTOR) + (DAMPING_FACTOR * sumMessages);
            vertex.setValue(new DoubleWritable(newPr));
        }

        // --- 判断是否继续迭代 ---
        if (getSuperstep() < MAX_SUPERSTEPS) {
            // 如果还没到 10 轮，就给邻居发消息
            long edgeCount = vertex.getNumEdges();
            if (edgeCount > 0) {
                // 计算要分发给每个邻居的值: 当前PR / 出度
                DoubleWritable msgToSend = new DoubleWritable(vertex.getValue().get() / edgeCount);
                
                // 发送给所有邻居 (广播)
                sendMessageToAllEdges(vertex, msgToSend);
            }
        } else {
            // --- 达到 10 轮，停止计算 ---
            vertex.voteToHalt();
        }
    }
}
package com.ecnu.pagerank.mr;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

public class PageRankMapper extends Mapper<LongWritable, Text, Text, Text> {
    
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // 输入格式: NodeID [TAB] CurrentPR [TAB] Target1,Target2...
        // 示例: 1    1.0    2,3
        
        String line = value.toString();
        String[] parts = line.split("\t"); 
        
        if (parts.length < 3) return; // 简单的容错

        String nodeId = parts[0];
        double currentPr = Double.parseDouble(parts[1]);
        String[] targets = parts[2].split(",");

        // 1. 传递图结构 (防止图在迭代中丢失)
        // 输出 Key: NodeID, Value: GRAPH_STR:2,3
        context.write(new Text(nodeId), new Text("GRAPH_STR:" + parts[2]));

        // 2. 分发 PR 值给邻居
        if (targets.length > 0) {
            double prContribution = currentPr / targets.length;
            for (String target : targets) {
                // 输出 Key: TargetID, Value: 0.5
                context.write(new Text(target), new Text(String.valueOf(prContribution)));
            }
        }
    }
}
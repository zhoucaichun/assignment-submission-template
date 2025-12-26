package com.ecnu.pagerank.mr;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import java.io.IOException;

public class PageRankReducer extends Reducer<Text, Text, Text, Text> {
    
    private static final double D = 0.85; // 阻尼系数

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        String targetNodes = "";
        double sumPr = 0.0;

        for (Text val : values) {
            String strVal = val.toString();
            if (strVal.startsWith("GRAPH_STR:")) {
                // 恢复图结构
                targetNodes = strVal.substring("GRAPH_STR:".length());
            } else {
                // 累加投票分值
                try {
                    sumPr += Double.parseDouble(strVal);
                } catch (NumberFormatException e) {
                    // 忽略异常数据
                }
            }
        }

        // 计算新 PR
        double newPr = (1.0 - D) + (D * sumPr);

        // 输出格式保持一致: NodeID [TAB] NewPR [TAB] Targets
        context.write(key, new Text(newPr + "\t" + targetNodes));
    }
}
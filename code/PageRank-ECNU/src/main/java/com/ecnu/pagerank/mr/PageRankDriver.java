package com.ecnu.pagerank.mr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

// 关键点1：继承 Configured 并实现 Tool 接口
public class PageRankDriver extends Configured implements Tool {

    private static final int MAX_ITERATIONS = 10;

    // 关键点2：使用 ToolRunner.run 来启动，这样 Hadoop 才能自动解析 -D 参数
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new PageRankDriver(), args);
        System.exit(res);
    }

    @Override
    public int run(String[] args) throws Exception {
        // 关键点3：检查参数数量
        if (args.length < 2) {
            System.err.println("Usage: PageRankDriver <input_path> <output_base_path>");
            System.err.println("Example: PageRankDriver /giraph/input/formatted_graph /giraph/output_mr/iter_");
            return -1; // 返回非0表示失败
        }

        String inputPath = args[0];     // 第一个参数是输入
        String basePath = args[1];      // 第二个参数是输出路径前缀

        // 获取经过 ToolRunner 解析过的 Configuration（包含了 -D 传进来的内存参数）
        Configuration conf = getConf(); 

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("Running Iteration " + (i + 1));

            // 使用 getConf() 创建 Job，确保继承了命令行的内存配置
            Job job = Job.getInstance(conf, "PageRank Iteration " + (i + 1));
            
            job.setJarByClass(PageRankDriver.class);
            job.setMapperClass(PageRankMapper.class);
            job.setReducerClass(PageRankReducer.class);

            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            // 逻辑：第0次读 inputPath，之后读 iter_(i-1)
            // 注意：如果上一轮输出是 /iter_0，这一轮输入就是 /iter_0
            String currentInput = (i == 0) ? inputPath : basePath + (i - 1);
            String currentOutput = basePath + i;
            
            // 为了防止路径出错，打印一下这一轮的输入输出
            System.out.println("  Input:  " + currentInput);
            System.out.println("  Output: " + currentOutput);

            FileInputFormat.addInputPath(job, new Path(currentInput));
            FileOutputFormat.setOutputPath(job, new Path(currentOutput));

            // 自动清理输出目录
            FileSystem fs = FileSystem.get(conf);
            Path outputPath = new Path(currentOutput);
            if (fs.exists(outputPath)) {
                fs.delete(outputPath, true);
            }

            boolean success = job.waitForCompletion(true);
            if (!success) {
                System.err.println("Iteration " + (i + 1) + " failed.");
                return 1;
            }
        }
        return 0;
    }
}
package com.ecnu.pagerank.mr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class PageRankDriver {

    private static final int MAX_ITERATIONS = 10; // 迭代10次

    public static void main(String[] args) throws Exception {
        // 这里的路径对应 HDFS 上的路径
        String inputPath = "/giraph/input/formatted_graph"; 
        String basePath = "/giraph/output_mr/iter_";

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("Running Iteration " + (i + 1));
            
            Configuration conf = new Configuration();
            // 每次创建一个新 Job
            Job job = Job.getInstance(conf, "PageRank Iteration " + (i + 1));
            
            job.setJarByClass(PageRankDriver.class);
            job.setMapperClass(PageRankMapper.class);
            job.setReducerClass(PageRankReducer.class);

            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            // 链式控制：第0次读 inputPath，之后读 iter_(i)
            String currentInput = (i == 0) ? inputPath : basePath + i;
            String currentOutput = basePath + (i + 1);
            
            FileInputFormat.addInputPath(job, new Path(currentInput));
            FileOutputFormat.setOutputPath(job, new Path(currentOutput));

            // 自动清理输出目录 (防止 FileAlreadyExistsException)
            FileSystem fs = FileSystem.get(conf);
            if (fs.exists(new Path(currentOutput))) {
                fs.delete(new Path(currentOutput), true);
            }

            boolean success = job.waitForCompletion(true);
            if (!success) {
                System.err.println("Iteration " + (i + 1) + " failed.");
                System.exit(1);
            }
        }
    }
}
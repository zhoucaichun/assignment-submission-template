**直接原因：内存不足 (OOM)。**

这是一个非常典型的错误：**“又要马儿跑，又要马儿不吃草”**。

* **RoadNet 数据集**：包含约 200 万个节点和 550 万条边，是一个中大型图。
* **Hadoop 默认内存**：你使用的“默认内存”只有 **1GB (1024MB)**，除去系统开销，Java 堆实际上只有 **200MB** 左右。

在 Giraph 中，图数据是必须**全部加载进内存**的。用 200MB 的内存去硬塞 RoadNet 这样的大图，Worker 会瞬间因为内存溢出（Out of Memory）而崩溃，导致任务失败。

---

###✅ 解决方案：显式申请 4GB 内存Fair 调度器（公平调度）的作用是**“谁先来谁先用，不够就排队”**，它不会自动把你的内存变大。你必须像之前一样，在命令里**强制要求**分配大内存。

请使用这条带有 **内存参数** 的命令（注意检查输入文件名是否正确）：

```bash
# 1. 先清理输出目录
hdfs dfs -rm -r /giraph/output_giraph_roadnet_fair

# 2. 提交任务 (带 4GB 内存参数)
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=4096 \
-Dmapreduce.map.java.opts=-Xmx3600m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA.json \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadnet_fair \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000

```

*(注意：请确认 `-vip` 后面的文件名是你 HDFS 上真实存在的 JSON 格式数据，比如 `roadNet-CA.json`，不要用那个报错的 `roadNet_mr.txt`)*

###🔍 为什么会失败？(技术细节)1. **Task failed**: 你的日志显示 `task_..._m_000003` 失败了。这是其中一个 Worker 节点。
2. **原因**: 当这个 Worker 试图读取一部分图数据时，发现内存满了（Heap Space Full）。它会拼命进行垃圾回收（GC），导致 CPU 飙升但无法继续工作，最终被 YARN 判定为“超时”或直接报错退出。
3. **Fair 调度器的角色**: 它只是负责把这 3 个任务分配给 ecnu02, 03, 04。如果你的任务申请的是 1GB，它就只给 1GB。它不会因为任务失败了就自动给你加内存。


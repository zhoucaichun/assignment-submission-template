

# FIFO 调度策略对比实验

**实验目标**：切换 Hadoop YARN 调度器为 **FIFO (先进先出)**，在不同数据集下重复 MapReduce 与 Giraph 的性能监控，并进行并发“排头阻塞”测试。

-----
## 开启history
检查Hadoop服务状态
```bash
# 01节点检查 HDFS 和 YARN 服务状态
jps
```
```bash
# 启动历史服务器
mr-jobhistory-daemon.sh start historyserver
```
---

## 第一阶段：修改配置 (Master 节点: ecnu01)

**目标**：将调度器从默认的 Capacity 切换为 FIFO。

1.  **编辑配置文件**

    ```bash
    nano /usr/local/hadoop/etc/hadoop/yarn-site.xml
    ```

      * 找到 `<name>yarn.resourcemanager.scheduler.class</name>`。
      * 将对应的 `<value>` 修改为：
        ```xml
        <value>org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler</value>
        ```
    
    ```bash 
     # 发给 ecnu02
    scp /usr/local/hadoop/etc/hadoop/yarn-site.xml root@ecnu02:/usr/local/hadoop/etc/hadoop/
    # 发给 ecnu03
    scp /usr/local/hadoop/etc/hadoop/yarn-site.xml root@ecnu03:/usr/local/hadoop/etc/hadoop/
    # 发给 ecnu04
    scp /usr/local/hadoop/etc/hadoop/yarn-site.xml root@ecnu04:/usr/local/hadoop/etc/hadoop/
    ```
2.  **重启 YARN 服务**

    ```bash
    /usr/local/hadoop/sbin/stop-yarn.sh
    /usr/local/hadoop/sbin/start-yarn.sh
    ```

3.  **验证状态**

      * 打开浏览器访问：`http://106.15.248.68:8088/cluster/scheduler`
      * 确认页面标题或左侧显示 **"FIFO Scheduler"**。

-----

## 第二阶段：MapReduce 性能监控 (FIFO版)

**准备工作**：

  * **窗口 01节点 (Master)**：用于提交任务。
  * **窗口 03节点 (Slave/ecnu03)**：用于运行 `dstat`。

<!-- end list -->

1. **打开03节点**
    ```bash
    ssh root@139.224.227.56
    ```

2. **启动监控 (03节点窗口)**

    ```bash
    #03监控的数据存到mr_stanford_fifo.csv文件里
    dstat -tcmnd --output mr_stanford_fifo.csv 1
    ```

3.  **打开另一个窗口，连接主节点01**
    ```bash
    ssh root@106.15.248.68
    ```

4.  **01窗口准备mapreduce任务**
    ```bash
    # 1. 彻底清理旧数据
    hdfs dfs -rm -r /giraph/output_mr
    # 2. 运行任务
    /usr/local/hadoop/bin/hadoop jar PageRank-ECNU-1.0-SNAPSHOT.jar \
    com.ecnu.pagerank.mr.PageRankDriver \
    -Dmapreduce.job.reduces=3 \
    -Dmapreduce.map.memory.mb=3072 \
    -Dmapreduce.map.java.opts=-Xmx2560m \
    -Dmapreduce.reduce.memory.mb=3072 \
    -Dmapreduce.reduce.java.opts=-Xmx2560m \
    -Dmapreduce.task.io.sort.mb=512 \
    /giraph/input/formatted_graph/stanford_mr.txt \
    /giraph/output_mr/iter_
    ```

5.  **执行流程**

      * 03 开始监控 -\> 01 提交任务 -\> 等待任务 Success -\> 03 停止。

-----

## 第三阶段：Giraph 性能监控 (FIFO版)
1.  **启动监控 (03节点窗口)**

    ```bash
    dstat -tcmnd --output giraph_stanford_fifo.csv 1
    ```

2.  **提交任务 (01节点窗口)**

    ```bash
    # 1. 设置环境变量
    export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/root/PageRank-ECNU-1.0-SNAPSHOT.jar

    # 2. 清理输出目录
    hdfs dfs -rm -r /giraph/output_giraph_stanford

    # 3. 提交 Giraph 任务
    /usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
    org.apache.giraph.GiraphRunner \
    -Dmapreduce.framework.name=yarn \
    -Dmapreduce.jobtracker.address=ecnu01:8032 \
    com.ecnu.pagerank.giraph.PageRankComputation \
    -vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
    -vip /giraph/input/stanford_input_json.txt \
    -vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
    -op /giraph/output_giraph_stanford \
    -w 3 \
    -ca giraph.SplitMasterWorker=true \
    -ca giraph.zkSessionMsecTimeout=600000
    ```

3.  **执行流程**

      * 同上，任务完成后停止监控。

-----

## 第四阶段 换大数据集RoadNet和小数据集random100  重复第二和三阶段的实验 

### 以大数据集为例子：
### 1：跑MapReduce （同中等数据集）

**准备工作**：
  * **窗口 03节点**：监控台
  * **窗口 01节点**：控制台

**1️⃣. 启动监控 (03节点窗口)**

```bash
  dstat -tcmnd --output mr_roadNet_fifo.csv 1
```

**2️⃣. 准备任务 (01节点窗口)**
```bash
  hdfs dfs -rm -r /giraph/output_mr

  /usr/local/hadoop/bin/hadoop jar PageRank-ECNU-1.0-SNAPSHOT.jar \
  com.ecnu.pagerank.mr.PageRankDriver \
  -Dmapreduce.job.reduces=3 \
  -Dmapreduce.map.memory.mb=3072 \
  -Dmapreduce.map.java.opts=-Xmx2560m \
  -Dmapreduce.reduce.memory.mb=3072 \
  -Dmapreduce.reduce.java.opts=-Xmx2560m \
  -Dmapreduce.task.io.sort.mb=512 \
  /giraph/input/formatted_graph/roadNet_mr.txt \
  /giraph/output_mr/iter_
```
```bash
  ##补充  小数据集：
  hdfs dfs -rm -r /giraph/output_mr

  /usr/local/hadoop/bin/hadoop jar PageRank-ECNU-1.0-SNAPSHOT.jar \
  com.ecnu.pagerank.mr.PageRankDriver \
  -Dmapreduce.job.reduces=3 \
  -Dmapreduce.map.memory.mb=3072 \
  -Dmapreduce.map.java.opts=-Xmx2560m \
  -Dmapreduce.reduce.memory.mb=3072 \
  -Dmapreduce.reduce.java.opts=-Xmx2560m \
  -Dmapreduce.task.io.sort.mb=512 \
  /giraph/input/formatted_graph/random100_mr.txt \
  /giraph/output_mr/iter_
```

**3️⃣. 执行流程**

  * 同上，任务完成后停止监控。

-----

### 2：跑Giraph （同中等数据集）

**1️⃣. 启动监控 (03节点窗口)**

```bash
dstat -tcmnd --output giraph_roadNet_fifo.csv 1
```

**2️⃣. 提交任务**

```bash
# 1. 设置环境变量
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/root/PageRank-ECNU-1.0-SNAPSHOT.jar

# 2. 清理输出目录
hdfs dfs -rm -r /giraph/output_giraph_roadNet

# 3. 提交 Giraph 任务 worker3_defaultS
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA_json.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadNet \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000
```

```bash
#补充 小数据集
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/root/PageRank-ECNU-1.0-SNAPSHOT.jar

hdfs dfs -rm -r /giraph/output_giraph_random100

/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/random_graph_100.txt \  #修改输入
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_random100 \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000
```


**3️⃣. 执行流程**

  * 同上，任务完成后停止监控。
-----

### 大数据集遇到的问题——giragh内存溢出与资源死锁
报错展示：
```text?code_stdout&code_event_index=1
             time  total usage:usr  total usage:sys  total usage:idl  total usage:wai  total usage:stl      used       free      buf       cach  net/total:recv  net/total:send  dsk/total:read  dsk/total:writ
0  15-12 14:38:20              NaN        980536.00      4724020.000          33280.0          1647500       NaN        NaN      NaN        NaN             NaN             NaN             NaN             NaN
1  15-12 14:38:21             0.50             0.25           99.012              0.0                0  980532.0  4724020.0  33280.0  1647504.0       12066.515       13624.711             0.0           0.000
2  15-12 14:38:22             0.00             0.00           98.763              0.0                0  980524.0  4724020.0  33288.0  1647504.0       10908.403       11408.468             0.0          52.007
3  15-12 14:38:23             0.75             0.25           99.256              0.0                0  980524.0  4724020.0  33288.0  1647504.0       10595.691       12053.786             0.0           0.000
4  15-12 14:38:24             0.00             0.00           99.202              0.0                0  980524.0  4724020.0  33288.0  1647504.0        2561.941        3440.661             0.0           0.000
Total rows captured: 49
```
####  1\. 分析
任务在大概 **48秒** 左右挂掉了。
  * **内存溢出 (OOM)**：RoadNet-CA 数据集比之前的 Web-Stanford 数据集大很多（120MB vs 38MB）。Giraph 是基于内存的，它试图把整个图加载到内存里。JVM 堆内存（Heap Size）设置得不够大，或者 YARN 容器给的内存不够，任务就会直接崩溃。
  * **ZooKeeper 超时**：虽然设置了 `zkSessionMsecTimeout`，但在大图加载或分区（Partitioning）阶段，如果网络或 CPU 繁忙，Worker 可能没能及时向 Master 汇报，导致 Master 判定 Worker 死亡。

#### 2\. 解决OOM——增加 YARN 容器内存
`-Dmapreduce.map.memory.mb=4096` (每个 Worker 4GB 内存)
`-Dmapreduce.map.java.opts=-Xmx3072m` (给 Java 堆内存 3GB)

#### 3\. 解决资源死锁——单 Worker 模式
扩容后，在实验过程中，多次观察到 Giraph 任务卡滞在启动阶段（Map 25%）。经分析，这是典型的**资源死锁 (Resource Deadlock)** 现象。
- **成因**：Giraph 采用 BSP 模型，要求所有 Worker 容器必须**同时**获得资源并从 Barrier（同步屏障）启动。当集群可用资源小于作业总资源需求时，部分容器（如 Master）会抢占并锁定资源，等待剩余容器启动；而剩余容器因资源被锁定无法分配，导致循环等待。
- **解决**：通过将作业配置降级为单 Worker 模式 (`-w 1`) ，将原子资源需求降低为 1 个容器，从而彻底规避了多容器间的资源依赖死锁。

---
## 第五阶段：FIFO 并发阻塞测试

**目标**：证明 FIFO 调度器存在排头阻塞现象（大作业未完成前，小作业无法开始）。

1.  **准备两个终端窗口**，均连接到 Master (`ecnu01`)。

2.  **窗口 1：提交大任务 (Giraph)**

      * 运行第三阶段的 Giraph 命令。

3.  **窗口 2：提交小任务 (MapReduce)**

      * 在 Giraph 开始运行后（约 5-10秒），立即提交一个小数据集任务：

4.  **截图**
      * 关注 YARN Web 界面 (`http://106.15.248.68:8088`)。
      * 发现 Giraph 任务状态为 `RUNNING`， MapReduce 任务状态为 **`ACCEPTED`**

5.  **实验结束**
      * 等待 Giraph 跑完，发现 MapReduce 瞬间变为 `RUNNING`。
-----
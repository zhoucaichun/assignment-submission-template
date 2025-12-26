

# FIFO 调度策略对比实验

**实验目标**：切换 Hadoop YARN 调度器为 **FIFO (先进先出)**，在同等数据集下重复 MapReduce 与 Giraph 的性能监控，并进行并发“排头阻塞”测试。

-----
## ui界面开启history
检查Hadoop服务状态
```bash
# 01节点查看是否开启，看结果是否有JobHistoryServer
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
      * 改完后按 Ctrl+O 保存，Enter 确认，Ctrl+X 退出
      * ps:三种调度我已经在xml中备注好了
    
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
    # 等待 5 秒
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
    #这个代码意思就是把03监控的数据存到建的mr_stanford_fifo.csv这个文件里
    dstat -tcmnd --output mr_stanford_fifo.csv 1
    ```

3.  **打开另一个窗口，连接主节点01**
    ```bash
    ssh root@106.15.248.68
    ```

4.  **01窗口准备mapreduce任务**
    ```bash
    # 1. 彻底清理旧数据（好习惯）
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

      * 窗口 B 回车 (开始监控) -\> 窗口 A 回车 (提交任务) -\> 等待任务 Success -\> 窗口 B 按 `Ctrl+C` 停止。

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

### 📣下面以大数据集为例子：
### 1：跑MapReduce （同中等数据集）

**准备工作**：
  * **窗口 03节点**：监控台
  * **窗口 01节点**：控制台

**1️⃣. 启动监控 (03节点窗口)**

```bash
dstat -tcmnd --output mr_roadNet_fifo.csv 1
```

**2️⃣. 准备任务 (01节点窗口)**
（注意修改input路径为了/input/formatted_graph/roadNet_mr.txt \）
```bash
    # 1. 彻底清理旧数据（好习惯）
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
    /giraph/input/formatted_graph/roadNet_mr.txt \
    /giraph/output_mr/iter_
```
```bash
    ##补充  小数据集的代码
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
    /giraph/input/formatted_graph/random100_mr.txt \
    /giraph/output_mr/iter_
```

**3️⃣. 执行流程**

  * 03节点回车 (开始监控) -\> 01节点回车 (提交任务) -\> **等待较长时间** (可能 \>10分钟) -\> 任务 Success -\> 03节点 `Ctrl+C`。

-----

### 2：跑Giraph （同中等数据集）

**1️⃣. 启动监控 (03节点窗口)**

```bash
dstat -tcmnd --output giraph_roadNet_fifo.csv 1
```

**2️⃣. 提交任务 (01节点窗口)**
*(注意：输入路径改为 `/giraph/input/roadNet-CA_json.txt`，输出路径改为 `output_giraph_roadNet`)*

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
#补充小数据集的giraph命令
# 1. 设置环境变量
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/root/PageRank-ECNU-1.0-SNAPSHOT.jar

# 2. 清理输出目录
hdfs dfs -rm -r /giraph/output_giraph_random100
# 3.跑实验
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/random_graph_100.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_random100 \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000
```


**3️⃣. 执行流程**

  * 03节点回车 -\> 01节点回车 -\> 等待 Success -\> 03节点 `Ctrl+C`。

-----

### ✅ 实验结束检查

做完这一套，你将得到两个新的大文件监控日志：

1.  `mr_roadNet_fifo.csv` (03节点输入命令 ls 查看是否有这个文件)
2.  `giraph_roadNet_fifo.csv` (03节点输入命令 ls 查看是否有这个文件)

**注意**：由于 RoadNet 数据集是真实路网图（不像 Web 图那样连接紧密），它的收敛速度和计算特征可能会有所不同，这正好能丰富你报告里“不同图结构对算法影响”的分析！

---
### 大数据集会遇到的问题——giragh内存不足问题
```python?code_reference&code_event_index=1
import pandas as pd

try:
    # Attempt to read the CSV file, skipping initial lines if necessary
    # Assuming dstat output structure similar to previous files
    df = pd.read_csv('giraph_roadNet_fifo.csv', header=5)
except:
    df = pd.read_csv('giraph_roadNet_fifo.csv', header=4)

# Display the first few rows to check if data was captured
print(df.head())

# Check the number of rows to determine duration
print(f"Total rows captured: {len(df)}")

```

```text?code_stdout&code_event_index=1
             time  total usage:usr  total usage:sys  total usage:idl  total usage:wai  total usage:stl      used       free      buf       cach  net/total:recv  net/total:send  dsk/total:read  dsk/total:writ
0  15-12 14:38:20              NaN        980536.00      4724020.000          33280.0          1647500       NaN        NaN      NaN        NaN             NaN             NaN             NaN             NaN
1  15-12 14:38:21             0.50             0.25           99.012              0.0                0  980532.0  4724020.0  33280.0  1647504.0       12066.515       13624.711             0.0           0.000
2  15-12 14:38:22             0.00             0.00           98.763              0.0                0  980524.0  4724020.0  33288.0  1647504.0       10908.403       11408.468             0.0          52.007
3  15-12 14:38:23             0.75             0.25           99.256              0.0                0  980524.0  4724020.0  33288.0  1647504.0       10595.691       12053.786             0.0           0.000
4  15-12 14:38:24             0.00             0.00           99.202              0.0                0  980524.0  4724020.0  33288.0  1647504.0        2561.941        3440.661             0.0           0.000
Total rows captured: 49

```

“Failed” 和 “有 CSV 结果” 并不矛盾，但这说明你的 Giraph 任务在运行过程中挂掉了。

根据你提供的 `giraph_roadNet_fifo.csv` 文件分析，我们可以找出失败的原因：

####  1\. 为什么显示 Failed？(根本原因)

任务虽然启动了，但在大概 **48秒** 左右挂掉了。

  * **内存溢出 (OOM)**：RoadNet-CA 数据集比之前的 Web-Stanford 数据集大很多（120MB vs 38MB）。Giraph 是基于内存的，它试图把整个图加载到内存里。如果你的 JVM 堆内存（Heap Size）设置得不够大，或者 YARN 容器给的内存不够，任务就会直接崩溃。
  * **ZooKeeper 超时**：虽然你设置了 `zkSessionMsecTimeout`，但在大图加载或分区（Partitioning）阶段，如果网络或 CPU 繁忙，Worker 可能没能及时向 Master 汇报心跳，导致 Master 判定 Worker 死亡。

#### 2\. 为什么还有 CSV 结果？

因为 `dstat` 是一个**独立运行的监控工具**。

  * 只要你在终端敲了回车启动它，它就开始每秒记录一次数据。
  * 它不管你的 Giraph 任务是成功还是失败，它只负责忠实地记录 `ecnu03` 节点的 CPU 和内存变化。
  * **你可以看到数据**：CSV 里有 49 行数据，说明它记录了任务启动后大约 49 秒的过程，直到你看到 Failed 后按下了 `Ctrl+C`。

#### 3\. 这个 CSV 还能用吗？

**部分可用，但不能代表完整性能。**

  * **前 40 秒的数据** 是真实的加载过程。你可以看到内存（`used` 列）在不断上涨，网络（`net` 列）有流量。
  * **缺失了核心计算部分**：因为任务在计算开始前或刚开始就挂了，你可能看不到之前那种漂亮的“网络脉冲”（Superstep 迭代特征），也看不到完整的收敛时间。

#### 4\. 怎么解决 Failed？（后面验证了这个解决方法还是不行，要增大内存基础上还要把w3改成w1）

如果要跑通这个 RoadNet 大数据集，你需要给 Giraph **“加内存”**。

请修改你的提交命令，增加内存参数：

**修改点 1：增加 YARN 容器内存**
在命令里加上：
`-Dmapreduce.map.memory.mb=4096` (给每个 Worker 4GB 内存，之前默认可能只有 1GB)
`-Dmapreduce.map.java.opts=-Xmx3072m` (给 Java 堆内存 3GB)

**修改后的完整命令 (试一下这个)：**

```bash
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=4096 \
-Dmapreduce.map.java.opts=-Xmx3072m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA_json.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadNet_v2 \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000
```

*(注意：我也把输出目录改成了 `_v2`，避免冲突)*

---
### 先尝试了扩大内存（0035），但是不行，然后用以下的代码在扩大内存基础上把w3变成w1就可以了：
```python?code_reference&code_event_index=1
import pandas as pd

try:
    # Attempt to read the CSV file
    df = pd.read_csv('giraph_roadNet_fifo_2.csv', header=5)
except:
    df = pd.read_csv('giraph_roadNet_fifo_2.csv', header=4)

# Basic stats
print(f"Total duration: {len(df)} seconds")
print(df.head())
print(df.tail())

# Calculate memory usage growth (used column usually in bytes, convert to GB)
df['used_GB'] = df['used'] / 1024 / 1024 / 1024
print(f"Start Memory: {df['used_GB'].iloc[0]:.2f} GB")
print(f"Peak Memory: {df['used_GB'].max():.2f} GB")
print(f"End Memory: {df['used_GB'].iloc[-1]:.2f} GB")

# Check CPU Wait (IO bottleneck indicator)
print(f"Average CPU Wait: {df['total usage:wai'].mean()}%")
print(f"Peak CPU Wait: {df['total usage:wai'].max()}%")

```

```text?code_stdout&code_event_index=1
Total duration: 498 seconds
             time  total usage:usr  total usage:sys  total usage:idl  total usage:wai  total usage:stl      used       free      buf       cach  net/total:recv  net/total:send  dsk/total:read  dsk/total:writ
0  15-12 15:22:23              NaN         951292.0      4375220.000          37368.0          2015392       NaN        NaN      NaN        NaN             NaN             NaN             NaN             NaN
1  15-12 15:22:24             0.00              0.0           98.759              0.0                0  951288.0  4375220.0  37368.0  2015396.0       10003.937       11534.081             0.0           0.000
2  15-12 15:22:25             0.25              0.0           99.013              0.0                0  951288.0  4375220.0  37368.0  2015396.0       14148.866       14676.936             0.0          44.006
3  15-12 15:22:26             0.50              0.0           99.239              0.0                0  951280.0  4375220.0  37376.0  2015396.0        2380.734        2776.690             0.0          47.995
4  15-12 15:22:27             0.50              0.0           99.493              0.0                0  951280.0  4375220.0  37376.0  2015396.0         278.981        1158.920             0.0           0.000
               time  total usage:usr  total usage:sys  total usage:idl  total usage:wai  total usage:stl      used       free      buf       cach  net/total:recv  net/total:send  dsk/total:read  dsk/total:writ
493  15-12 15:30:36            0.750             0.25           99.249              0.0                0  965112.0  4313120.0  38148.0  2061564.0       10801.885        5816.938             0.0         175.998
494  15-12 15:30:37            0.250             0.00           99.000              0.0                0  965112.0  4313120.0  38148.0  2061564.0         413.000        4903.000             0.0           8.000
495  15-12 15:30:38            0.000             0.25           98.999              0.0                0  960744.0  4363352.0  38148.0  2016856.0         202.999         634.996             0.0           0.000
496  15-12 15:30:39            0.250             0.75           99.498              0.0                0  960744.0  4363352.0  38148.0  2016856.0         440.993        1325.978             0.0           0.000
497  15-12 15:30:40            0.246             0.00           97.791              0.0                0  960744.0  4363352.0  38148.0  2016856.0        6188.682        6604.480             0.0           3.941
Start Memory: nan GB
Peak Memory: 0.00 GB
End Memory: 0.00 GB
Average CPU Wait: 75.06274899598394%
Peak CPU Wait: 37368.0%

```

你上传的 `giraph_roadNet_fifo_2.csv` 文件显示任务依然失败，但这次的情况和上次（48秒挂掉）完全不同。这次坚持了 **498秒 (8分多钟)** 才结束！

从数据中我们发现了 **新的致命问题**，而且这个问题非常隐蔽：

#### 1\. 致命异常：CPU Wait 爆表 (Wait Time: 75%)

  * **数据证据**：`Average CPU Wait: 75.06%`。
  * **这意味着什么？**：
      * 正常计算任务（如 PageRank）的 CPU 应该忙于计算 (`usr`)，而不是等待 (`wai`)。
      * **75% 的时间** CPU 都在傻等！它在等什么？**等 Swap（虚拟内存）交换**。
  * **场景重现**：
      * 你给 Giraph 分配了 **4GB 内存** (`-Dmapreduce.map.memory.mb=4096`)。
      * 但是！你的服务器节点（ecnu03）可能**物理内存本身就吃紧**。
      * 当 Java 进程真的去要这 4GB 内存时，操作系统发现物理内存不够了，于是开始疯狂地把内存数据写到硬盘的 Swap 分区里，然后再读回来。
      * 这种 **"内存颠簸" (Thrashing)** 导致速度慢如蜗牛，最后因为超时（Timeout）被 YARN 杀掉。

#### 2\. 内存并没有真正涨上去

  * **数据证据**：`Peak Memory` 只有 **0.00 GB**（这里可能是 dstat 单位换算问题，但看原始数据 `used` 只有 `960744.0`，也就是 900MB 左右）。
  * **分析**：这说明 Java 进程虽然申请了 4GB 的上限，但实际还没用到那么多，机器就已经卡死了。或者 dstat 监控的 `used` 包含了被缓存占用的内存，导致你看不到真实的进程内存增长。

#### 3\. 终极解决方案：给 Giraph "减负" (Downsize)

既然“加内存”会导致机器卡死（Thrashing），我们需要反其道而行之：**减少并行度，让单机独享资源**。

这个 RoadNet 数据集有 200万节点，其实 **1 个 Worker 给 3GB 内存** 是完全能跑的，不需要 3 个 Worker 并行（那样会由 3 个 JVM 抢资源）。

**请尝试这个“单兵作战”命令：**

1.  **减少 Worker**：`-w 1` (只用 1 个 Worker，避免多进程打架)。
2.  **取消分离**：`SplitMasterWorker=false` (Master 和 Worker 合体，省掉 1 个容器)。
3.  **内存适中**：给 3GB 堆内存，预留一点给系统。

**复制这个命令重跑 (最后一次尝试)：**

```bash
# 1. 清理输出目录
hdfs dfs -rm -r /giraph/output_giraph_roadNet_v3

# 2. 提交任务 (关键：保留了 4GB 内存参数，同时限制 -w 1)
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=4096 \
-Dmapreduce.map.java.opts=-Xmx3500m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA_json.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadNet_v3 \
-w 1 \
-ca giraph.SplitMasterWorker=false \
-ca giraph.zkSessionMsecTimeout=600000
```

---
## 第四阶段：FIFO 专属“高分”实验 (并发阻塞测试)

**目标**：证明 FIFO 调度器存在“排头阻塞”现象（大作业未完成前，小作业无法开始）。

1.  **准备两个终端窗口**，均连接到 Master (`ecnu01`)。

2.  **窗口 1：提交大任务 (Giraph)**

      * 直接运行第三阶段的 Giraph 提交命令。

3.  **窗口 2：立即提交小任务 (MapReduce)**

      * 在 Giraph 开始运行后（约 5-10秒），立即提交一个小数据集任务：

    <!-- end list -->

    ```bash
    # 确保小数据在 formatted_graph 目录，或者直接用 random_graph_100
    # 这里假设 Driver 默认读的是 formatted_graph (里边是大文件)，你可以临时指定一个小文件
    # 或者直接提交这个命令，反正它会卡住：
    /usr/local/hadoop/bin/hadoop jar PageRank-ECNU-1.0-SNAPSHOT.jar com.ecnu.pagerank.mr.PageRankDriver
    ```

    *(注：如果没有小数据，直接再次提交 MapReduce 大任务也可以，只要能看到排队就行)*

4.  **📸 关键截图时刻**

      * 刷新 YARN Web 界面 (`http://106.15.248.68:8088`)。
      * **现象**：你会看到 Giraph 任务状态为 `RUNNING`，而刚才提交的 MapReduce 任务状态为 **`ACCEPTED`**（注意：不是 RUNNING，且进度条不动）。
      * **截图要求**：截取包含这两个任务状态的列表，证明 MapReduce 被 Giraph “堵”住了。

5.  **实验结束**

      * 等待 Giraph 跑完，你会发现 MapReduce 瞬间变为 `RUNNING`。

-----
# 大数据集进行giraph内存不足
## 运行的代码是：
```bash
# 1. 设置环境变量
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/root/PageRank-ECNU-1.0-SNAPSHOT.jar

# 2. 清理输出目录
hdfs dfs -rm -r /giraph/output_giraph_roadNet

# 3. 提交 Giraph 任务
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
## 查看03节点失败日志
进入目录：
```bash
cd /usr/local/hadoop/logs/userlogs/
```
定位到具体的 Container根据你之前的报错 `application_1765879591545_0022`，执行：

```bash
cd application_1765879591545_0022
ls
```
```bash
# 名字大概长这样，你需要用 tab 键补全
cd container_1765879591545_0022_01_000003/  

```
查看“死亡原因”在这个目录下，通常有三个文件：

* `stdout`: 标准输出（你的 `System.out.println` 都在这）。
* `stderr`: 标准错误（报错信息都在这）。
* `syslog`: 系统日志（Hadoop/Giraph 的运行日志，信息最全）。

```bash
# 查看最后 100 行错误日志
tail -n 100 syslog
tail -n 100 stderr

```
结果
```bash
2025-12-16 19:15:55,464 INFO [main] org.apache.giraph.comm.netty.NettyServer: NettyServer: Using execution group with 8 threads for requestFrameDecoder.
2025-12-16 19:15:55,522 INFO [main] org.apache.hadoop.conf.Configuration.deprecation: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
2025-12-16 19:15:55,607 INFO [main] org.apache.giraph.comm.netty.NettyServer: start: Started server communication server: ecnu03/172.26.142.120:30001 with up to 16 threads on bind attempt 0 with sendBufferSize = 32768 receiveBufferSize = 524288
2025-12-16 19:15:55,617 INFO [main] org.apache.giraph.comm.netty.NettyClient: NettyClient: Using execution handler with 8 threads after request-encoder.
2025-12-16 19:15:55,636 INFO [main] org.apache.giraph.graph.GraphTaskManager: setup: Registering health of this worker...
2025-12-16 19:15:55,649 INFO [main] org.apache.giraph.bsp.BspService: getJobState: Job state already exists (/_hadoopBsp/job_1765879591545_0022/_masterJobState)
2025-12-16 19:15:55,654 INFO [main] org.apache.giraph.bsp.BspService: getApplicationAttempt: Node /_hadoopBsp/job_1765879591545_0022/_applicationAttemptsDir already exists!
2025-12-16 19:15:55,662 INFO [main] org.apache.giraph.worker.BspServiceWorker: registerHealth: Created my health node for attempt=0, superstep=-1 with /_hadoopBsp/job_1765879591545_0022/_applicationAttemptsDir/0/_superstepDir/-1/_workerHealthyDir/ecnu03_1 and workerInfo= Worker(hostname=ecnu03 hostOrIp=ecnu03, MRtaskID=1, port=30001)
2025-12-16 19:15:55,791 INFO [netty-server-worker-0] org.apache.giraph.comm.netty.NettyServer: start: Using Netty without authentication.
2025-12-16 19:15:55,855 INFO [netty-server-worker-0] org.apache.giraph.comm.netty.handler.RequestDecoder: decode: Server window metrics MBytes/sec received = 0, MBytesReceived = 0.0004, ave received req MBytes = 0.0004, secs waited = 1.76588378E9
2025-12-16 19:15:55,866 INFO [main] org.apache.giraph.worker.BspServiceWorker: startSuperstep: Master(hostname=ecnu02, MRtaskID=0, port=30000)
2025-12-16 19:15:55,869 INFO [main] org.apache.giraph.partition.WorkerGraphPartitionerImpl: After updating partitionOwnerList 3 workers are available
2025-12-16 19:15:55,879 INFO [netty-client-worker-0] org.apache.giraph.comm.netty.NettyClient: Using Netty without authentication.
2025-12-16 19:15:55,890 INFO [netty-client-worker-1] org.apache.giraph.comm.netty.NettyClient: Using Netty without authentication.
2025-12-16 19:15:55,906 INFO [netty-client-worker-2] org.apache.giraph.comm.netty.NettyClient: Using Netty without authentication.
2025-12-16 19:15:55,930 INFO [netty-server-worker-1] org.apache.giraph.comm.netty.NettyServer: start: Using Netty without authentication.
2025-12-16 19:15:55,945 INFO [netty-server-worker-2] org.apache.giraph.comm.netty.NettyServer: start: Using Netty without authentication.
2025-12-16 19:15:55,958 INFO [main] org.apache.giraph.comm.netty.NettyClient: connectAllAddresses: Successfully added 3 connections, (3 total connected) 0 failed, 0 failures total.
2025-12-16 19:15:55,973 INFO [main] org.apache.giraph.worker.BspServiceWorker: loadInputSplits: Using 1 thread(s), originally 1 threads(s)
2025-12-16 19:15:56,000 INFO [load-0] org.apache.giraph.worker.InputSplitsCallable: call: Loaded 0 input splits in 0.025949374 secs, (v=0, e=0) 0.0 vertices/sec, 0.0 edges/sec
2025-12-16 19:15:56,004 INFO [main] org.apache.giraph.comm.netty.NettyClient: waitAllRequests: Finished all requests. MBytes/sec received = 0.0012, MBytesReceived = 0, ave received req MBytes = 0, secs waited = 0.012
MBytes/sec sent = 0.0019, MBytesSent = 0, ave sent req MBytes = 0, secs waited = 0.013
2025-12-16 19:15:56,004 INFO [main] org.apache.giraph.worker.BspServiceWorker: setup: Finally loaded a total of (v=0, e=0)
2025-12-16 19:15:57,372 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 18ms
2025-12-16 19:16:00,652 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 63ms
2025-12-16 19:16:00,655 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS MarkSweep, action = end of major GC, cause = Ergonomics, duration = 224ms
2025-12-16 19:16:01,851 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 35ms
2025-12-16 19:16:02,995 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 58ms
2025-12-16 19:16:04,694 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 62ms
2025-12-16 19:16:05,897 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 76ms
2025-12-16 19:16:08,440 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS Scavenge, action = end of minor GC, cause = Allocation Failure, duration = 128ms
2025-12-16 19:16:08,441 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS MarkSweep, action = end of major GC, cause = Ergonomics, duration = 1008ms
2025-12-16 19:16:13,817 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS MarkSweep, action = end of major GC, cause = Ergonomics, duration = 698ms
2025-12-16 19:16:13,832 INFO [main-EventThread] org.apache.giraph.bsp.BspService: process: all input splits done
2025-12-16 19:16:13,978 INFO [main] org.apache.giraph.worker.BspServiceWorker: finishSuperstep: Waiting on all requests, superstep -1 Memory (free/total/max) = 39.43M / 178.00M / 178.00M
2025-12-16 19:16:13,978 INFO [main] org.apache.giraph.comm.netty.NettyClient: waitAllRequests: Finished all requests. MBytes/sec received = 0, MBytesReceived = 0, ave received req MBytes = 0, secs waited = 17.987
MBytes/sec sent = 0, MBytesSent = 0, ave sent req MBytes = 0, secs waited = 17.987
2025-12-16 19:16:13,978 INFO [main] org.apache.giraph.worker.WorkerAggregatorHandler: finishSuperstep: Start gathering aggregators, workers will send their aggregated values once they are done with superstep computation
2025-12-16 19:16:14,005 INFO [main] org.apache.giraph.comm.netty.NettyClient: logInfoAboutOpenRequests: Waiting interval of 15000 msecs, 0 open requests, MBytes/sec received = 0.0025, MBytesReceived = 0, ave received req MBytes = 0, secs waited = 0.006
MBytes/sec sent = 0.0034, MBytesSent = 0, ave sent req MBytes = 0, secs waited = 0.006
2025-12-16 19:16:14,006 INFO [main] org.apache.giraph.comm.netty.NettyClient: logInfoAboutOpenRequests:
2025-12-16 19:16:14,007 INFO [main] org.apache.giraph.comm.netty.NettyClient: waitAllRequests: Finished all requests. MBytes/sec received = 0.0017, MBytesReceived = 0, ave received req MBytes = 0, secs waited = 0.008
MBytes/sec sent = 0.0026, MBytesSent = 0, ave sent req MBytes = 0, secs waited = 0.008
2025-12-16 19:16:14,007 INFO [main] org.apache.giraph.worker.BspServiceWorker: finishSuperstep: Superstep -1, messages = 0 , message bytes = 0 , Memory (free/total/max) = 38.68M / 178.00M / 178.00M
2025-12-16 19:16:14,031 INFO [main] org.apache.giraph.worker.BspServiceWorker: Writing counters to zookeeper for superstep: -1
2025-12-16 19:16:14,067 INFO [main] org.apache.giraph.worker.BspServiceWorker: finishSuperstep: (waiting for rest of workers) WORKER_ONLY - Attempt=0, Superstep=-1
2025-12-16 19:16:14,097 INFO [main-EventThread] org.apache.giraph.bsp.BspService: process: superstepFinished signaled
2025-12-16 19:16:14,114 INFO [main] org.apache.giraph.worker.BspServiceWorker: finishSuperstep: Completed superstep -1 with global stats (vtx=1965206,finVtx=0,edges=5533214,msgCount=0,msgBytesCount=0,haltComputation=false, checkpointStatus=NONE) and classes (computation=com.ecnu.pagerank.giraph.PageRankComputation,incoming=org.apache.giraph.conf.DefaultMessageClasses@720653c2,outgoing=org.apache.giraph.conf.DefaultMessageClasses@45f24169)
2025-12-16 19:16:14,120 WARN [main-EventThread] org.apache.giraph.bsp.BspService: process: Unknown and unprocessed event (path=/_hadoopBsp/job_1765879591545_0022/_applicationAttemptsDir/0/_superstepDir, type=NodeChildrenChanged, state=SyncConnected)
2025-12-16 19:16:15,134 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS MarkSweep, action = end of major GC, cause = Ergonomics, duration = 991ms
2025-12-16 19:16:18,414 FATAL [main] org.apache.hadoop.mapred.YarnChild: Error running child : java.lang.OutOfMemoryError: Java heap space
        at it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.<init>(Long2ObjectOpenHashMap.java:107)
        at it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.<init>(Long2ObjectOpenHashMap.java:115)
        at org.apache.giraph.types.ops.collections.Basic2ObjectMap$BasicLong2ObjectOpenHashMap.<init>(Basic2ObjectMap.java:294)
        at org.apache.giraph.types.ops.LongTypeOps.create2ObjectOpenHashMap(LongTypeOps.java:92)
        at org.apache.giraph.types.ops.LongTypeOps.create2ObjectOpenHashMap(LongTypeOps.java:33)
        at org.apache.giraph.comm.messages.primitives.IdByteArrayMessageStore.<init>(IdByteArrayMessageStore.java:108)
        at org.apache.giraph.comm.messages.InMemoryMessageStoreFactory.newStoreWithoutCombiner(InMemoryMessageStoreFactory.java:135)
        at org.apache.giraph.comm.messages.InMemoryMessageStoreFactory.newStore(InMemoryMessageStoreFactory.java:163)
        at org.apache.giraph.comm.messages.InMemoryMessageStoreFactory.newStore(InMemoryMessageStoreFactory.java:51)
        at org.apache.giraph.comm.ServerData.prepareSuperstep(ServerData.java:267)
        at org.apache.giraph.comm.netty.NettyWorkerServer.prepareSuperstep(NettyWorkerServer.java:97)
        at org.apache.giraph.worker.BspServiceWorker.startSuperstep(BspServiceWorker.java:720)
        at org.apache.giraph.graph.GraphTaskManager.execute(GraphTaskManager.java:333)
        at org.apache.giraph.graph.GraphMapper.run(GraphMapper.java:90)
        at org.apache.hadoop.mapred.MapTask.runNewMapper(MapTask.java:787)
        at org.apache.hadoop.mapred.MapTask.run(MapTask.java:341)
        at org.apache.hadoop.mapred.YarnChild$2.run(YarnChild.java:164)
        at java.security.AccessController.doPrivileged(Native Method)
        at javax.security.auth.Subject.doAs(Subject.java:422)
        at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1698)
        at org.apache.hadoop.mapred.YarnChild.main(YarnChild.java:158)

2025-12-16 19:16:18,421 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS MarkSweep, action = end of major GC, cause = Ergonomics, duration = 1381ms
2025-12-16 19:16:18,427 INFO [Service Thread] org.apache.giraph.graph.GraphTaskManager: installGCMonitoring: name = PS MarkSweep, action = end of major GC, cause = Allocation Failure, duration = 1892ms
2025-12-16 19:16:18,447 INFO [communication thread] org.apache.hadoop.mapred.Task: Communication exception: java.io.IOException: Failed on local exception: java.io.InterruptedIOException: Interrupted while waiting for IO on channel java.nio.channels.SocketChannel[connected local=/172.26.142.120:56262 remote=/172.26.142.120:39791]. 60000 millis timeout left.; Host Details : local host is: "ecnu03/172.26.142.120"; destination host is: "ecnu03":39791;
        at org.apache.hadoop.net.NetUtils.wrapException(NetUtils.java:773)
        at org.apache.hadoop.ipc.Client.call(Client.java:1479)
        at org.apache.hadoop.ipc.Client.call(Client.java:1412)
        at org.apache.hadoop.ipc.WritableRpcEngine$Invoker.invoke(WritableRpcEngine.java:242)
        at com.sun.proxy.$Proxy9.ping(Unknown Source)
        at org.apache.hadoop.mapred.Task$TaskReporter.run(Task.java:767)
        at java.lang.Thread.run(Thread.java:750)
Caused by: java.io.InterruptedIOException: Interrupted while waiting for IO on channel java.nio.channels.SocketChannel[connected local=/172.26.142.120:56262 remote=/172.26.142.120:39791]. 60000 millis timeout left.
        at org.apache.hadoop.net.SocketIOWithTimeout$SelectorPool.select(SocketIOWithTimeout.java:342)
        at org.apache.hadoop.net.SocketIOWithTimeout.doIO(SocketIOWithTimeout.java:157)
        at org.apache.hadoop.net.SocketInputStream.read(SocketInputStream.java:161)
        at org.apache.hadoop.net.SocketInputStream.read(SocketInputStream.java:131)
        at java.io.FilterInputStream.read(FilterInputStream.java:133)
        at java.io.FilterInputStream.read(FilterInputStream.java:133)
        at org.apache.hadoop.ipc.Client$Connection$PingInputStream.read(Client.java:520)
        at java.io.BufferedInputStream.fill(BufferedInputStream.java:246)
        at java.io.BufferedInputStream.read(BufferedInputStream.java:265)
        at java.io.DataInputStream.readInt(DataInputStream.java:387)
        at org.apache.hadoop.ipc.Client$Connection.receiveRpcResponse(Client.java:1084)
        at org.apache.hadoop.ipc.Client$Connection.run(Client.java:979)

2025-12-16 19:16:18,535 INFO [main] org.apache.hadoop.metrics2.impl.MetricsSystemImpl: Stopping MapTask metrics system...
2025-12-16 19:16:18,536 INFO [main] org.apache.hadoop.metrics2.impl.MetricsSystemImpl: MapTask metrics system stopped.
2025-12-16 19:16:18,536 INFO [main] org.apache.hadoop.metrics2.impl.MetricsSystemImpl: MapTask metrics system shutdown complete.
SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/usr/local/hadoop/tmp/nm-local-dir/usercache/root/appcache/application_1765879591545_0022/filecache/11/job.jar/job.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/usr/local/hadoop/share/hadoop/common/lib/slf4j-log4j12-1.7.10.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.Log4jLoggerFactory]
```
分析：
请看日志里第 88 行，那行红色的致命错误：

> **`2025-12-16 19:16:18,414 FATAL ... Error running child : java.lang.OutOfMemoryError: Java heap space`**

###1. **死因明确**：**OOM (内存溢出)**。
* 你的 Java 堆内存（Heap Space）被撑爆了。


2. **挣扎过程**：
* 在死前的几秒钟里，JVM 疯狂地进行垃圾回收 (GC) 试图“续命”。
* `19:16:08` -> **Full GC** (停顿 1秒)
* `19:16:13` -> **Full GC** (停顿 0.7秒)
* `19:16:15` -> **Full GC** (停顿 1秒)
* `19:16:18` -> **彻底崩溃**。
* 这就是典型的“垂死挣扎”。


3. **根本原因**：
* 请看这行日志：`Memory (free/total/max) = 38.68M / 178.00M / 178.00M`
* **178MB ？？？**
* 因为你之前的命令里没加内存参数，YARN 给你的 Container 分配的默认堆内存竟然只有 **178MB**。
* 而 RoadNet-CA 图数据加载进去至少需要几百 MB，这点内存连塞牙缝都不够。



---

###✅ 最终解决方案:**加" 4GB 内存"的命令跑！**

```bash
# 1. 清理目录
hdfs dfs -rm -r /giraph/output_giraph_roadNet

# 2. 满血版命令 (给 4GB 内存)
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=4096 \
-Dmapreduce.map.java.opts=-Xmx3600m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA_json.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadNet \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000

```

# 失败---原因分析

一直卡在 **map 25%** 是 Giraph 任务遭遇**“资源死锁 (Resource Deadlock)”**的最典型特征。

###🕵️‍♂️ 为什么是 25%？这道数学题很简单：

* **你的配置**：`-w 3` 且 `SplitMasterWorker=true`。这意味着你需要 **4 个容器**（1 个 Master + 3 个 Worker）。
* **当前状态**：只有 **1 个容器** 成功抢到了资源并跑起来了（就是那个 Master）。
* **计算进度**：1 \div 4 = 25\%。
* **卡住原因**：
* 这个跑起来的 Master 正在喊：“兄弟们（Workers）都连上来了吗？人齐了我们就开工！”
* 剩下的 3 个 Worker 却在排队：“老大，我们还没拿到入场券（内存），YARN 说没资源了！”
* **结果**：Master 占着资源干等，Worker 等不到资源。互相死锁。



---

###✂️ 第一步：立刻终止任务不要浪费时间等它报错，手动杀掉：

```bash
# 你的 ID 是 0023
yarn application -kill application_1765879591545_0023

```

---

###📉 第二步：降级配置（必须妥协）根据你上一张截图，集群只剩下 **2GB** 的空闲资源。你要跑 4 个 4GB 的节点（共 16GB）是绝对不可能塞进去的。

我们必须**同时减少人数**和**降低内存**，才能让任务挤进去。

####推荐方案：2 个 Worker + 3GB 内存我们把原来的“4人团”缩减为“3人团”（1 Master + 2 Worker），并且每人少吃点。

* **`-w 2`**: 只需要 2 个 Worker。
* **`memory.mb=3072`**: 降到 3GB。

请使用这条新命令：

```bash
# 1. 清理目录
hdfs dfs -rm -r /giraph/output_giraph_roadNet

# 2. 降级版命令 (更容易跑通)
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=3072 \
-Dmapreduce.map.java.opts=-Xmx2560m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA_json.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadNet \
-w 2 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000

```
----
# 失败——原因分析

请看这一行最关键的数据：
> **Application Attempt Headroom : `<memory:0, vCores:0>**`

**这意味着：集群的空闲资源是 0。** 一滴都没有了。

###💀 为什么会死锁？1. **你的需求**：你还缺 **2 个** 容器（Outstanding Resource Requests）。
2. **现有资源**：你已经占用了 **2 个** 容器（Total Allocated）。
3. **死局**：你的任务占了一半资源在等另一半，但另一半资源没有了（可能被其他后台进程占了，或者你的机器本来就只有这么大）。
* 你的 Master 拿着资源不肯放，Worker 拿不到资源进不来。这就是典型的**资源死锁**。



---

###✅ 终极解决方案：单兵作战模式 (Single Node)既然你的集群挤不下“团队作战”（多节点），那我们就开启 **“单兵模式”**。

我们将做一个极端的优化：**把 Master 和 Worker 合并到同一个容器里，只用 1 个容器跑完全程！**

这样只要能启动一个容器，任务就一定能跑完，绝对不会死锁。

####1. 杀掉当前任务```bash
yarn application -kill application_1765879591545_0025

```

####2. 清理目录```bash
hdfs dfs -rm -r /giraph/output_giraph_roadNet

```

####3. 执行“求生版”命令**关键改动**：

* **`-w 1`**: 只要 1 个 Worker。
* **`giraph.SplitMasterWorker=false`**: **关键！** 让 Master 和 Worker 住在同一个屋檐下，不分开占用两个容器。
* **结果**：整个任务只需要 **1 个 4GB 的容器** 就能跑。如果这都跑不起来，那你的集群可能需要重启了。

```bash
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=4096 \
-Dmapreduce.map.java.opts=-Xmx3600m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/roadNet-CA_json.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_giraph_roadNet \
-w 1 \
-ca giraph.SplitMasterWorker=false \
-ca giraph.zkSessionMsecTimeout=600000

```
----
# 为什么会发生死锁
这是一个非常深刻的问题。你遇到的死锁并非代码逻辑错误（比如 Java 里的 `synchronized` 死锁），而是分布式计算中特有的 **“资源死锁” (Resource Deadlock)**，学术上通常称为 **“群组调度 (Gang Scheduling) 问题”**。

简单来说，就是：**“想凑一桌麻将，但只抢到了 3 个椅子，坐下的人不肯走，站着的人坐不下，大家就这样僵住了。”**

我来为你深度复盘为什么在你的集群环境下，这个死锁一直发生：

###1. 核心矛盾：Giraph 的“同进同退” vs YARN 的“先到先得”这起事故的根源在于 Giraph 和 MapReduce 的运行机制完全不同：

* **MapReduce (独狼模式)**：
* 假设你有 100 个 Map 任务，但集群只能同时跑 2 个。
* 没关系！YARN 会先跑 2 个，跑完释放资源，再跑下 2 个。**只要有一点资源，任务就能慢慢挪完。**


* **Giraph (团队模式)**：
* Giraph 是基于 **BSP (整体同步并行)** 模型的。
* 它要求：**所有的 Worker 必须同时在线，才能开始第一轮计算 (Superstep 0)。**
* 如果你申请了 4 个 Worker，但 YARN 只给了你 3 个，那这 3 个 Worker 会原地待命，无限期等待第 4 个兄弟。它们**绝对不会**先开始干活。



###2. 死锁现场还原 (The Trap)让我们回到你卡在 25% 进度的那个场景：

* **你的需求**：你要 4 个容器（1 Master + 3 Worker），每个 4GB。总需求 **16GB**。
* **集群现状**：假设集群总空闲资源只有 **8GB**。

**死锁过程如下：**

1. **分配开始**：YARN 看到有两个 4GB 的空位，于是先把 **Master** 和 **Worker 1** 启动了。
2. **资源占用**：这两个容器立刻占用了 8GB 内存。此时，**集群空闲资源 = 0**。
3. **无尽等待**：
* **Master & Worker 1**：它们占着茅坑不拉屎，因为它们在等 Worker 2 和 Worker 3 上线才能开始同步。
* **Worker 2 & Worker 3**：它们在排队等资源。但是资源被 Master 和 Worker 1 占着。


4. **死局**：
* Master 不释放资源，因为没算完。
* 新资源进不来，因为 Master 不释放。
* **结果：一直卡住 (Pending)，直到天荒地老。**



###3. 为什么你的“多节点”尝试总失败？因为你的集群资源**太碎、太少**了。

* **碎片化 (Fragmentation)**：
* 你说集群里有“空闲资源”，但它们可能分散在不同机器上。
* 如果有两台机器，A 剩 2GB，B 剩 2GB。虽然总共剩 4GB，但你**塞不进一个 4GB 的容器**（因为容器不能跨机器拆分）。


* **Headroom (额度) 欺骗**：
* 你看到的 `Headroom: <memory:2048>` 意味着你离死锁只有一步之遥。只要有一个容器启动稍微慢一点，或者有别的同学提交了一个小任务，资源链条瞬间断裂。



###4. 为什么刚才的“终极方案”成功了？刚才我让你用的配置是：

* **`-w 1` (单 Worker)**
* **`SplitMasterWorker=false` (Master 不独占容器)**

这个配置的精妙之处在于：**它打破了“凑桌子”的困境。**

* **总需求**：只需要 **1 个** 容器（Master 和 Worker 合体）。
* **逻辑**：只要 YARN 能给你分配 **1 个** 容器，这个容器自己既是裁判（Master）又是运动员（Worker）。
* **结果**：人齐了（就它自己），直接开工！**根本不需要等待其他资源，所以永远不会死锁。**

###📝 总结 (写进报告的加分项)> **关于资源死锁的分析：**
> 在实验过程中，多次观察到 Giraph 任务卡滞在启动阶段（Map 25%）。经分析，这是典型的**资源死锁 (Resource Deadlock)** 现象。
> **成因**：Giraph 采用 BSP 模型，要求所有 Worker 容器必须**同时**获得资源并从 Barrier（同步屏障）启动。当集群可用资源小于作业总资源需求时，部分容器（如 Master）会抢占并锁定资源，等待剩余容器启动；而剩余容器因资源被锁定无法分配，导致循环等待。
> **解决**：通过将作业配置降级为单 Worker 模式 (`-w 1`) 并关闭 Master 独占 (`SplitMasterWorker=false`)，将原子资源需求降低为 1 个容器，从而彻底规避了多容器间的资源依赖死锁。



----
# 小数据集 worker3——1G 跑
针对这个只有 **6.9KB (6984 bytes)** 的超小数据集 `random_graph_100.txt`，我们可以把配置降到最低，这样任务启动最快，且完全不会占用集群资源。
 **3 个 Worker** 来验证分布式逻辑

###运行命令 (极速版)对于 7KB 的数据，**1GB 内存** 都是杀鸡用牛刀了，但这是 Hadoop 的最小单位。

```bash
# 1. 清理输出
hdfs dfs -rm -r /giraph/output_random_100

# 2. 提交任务 (3 Worker, 最小内存)
/usr/local/hadoop/bin/hadoop jar /root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar \
org.apache.giraph.GiraphRunner \
-Dmapreduce.framework.name=yarn \
-Dmapreduce.jobtracker.address=ecnu01:8032 \
-Dmapreduce.map.memory.mb=1024 \
-Dmapreduce.map.java.opts=-Xmx900m \
com.ecnu.pagerank.giraph.PageRankComputation \
-vif org.apache.giraph.io.formats.JsonLongDoubleFloatDoubleVertexInputFormat \
-vip /giraph/input/random_graph_100.txt \
-vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
-op /giraph/output_random_100 \
-w 3 \
-ca giraph.SplitMasterWorker=true \
-ca giraph.zkSessionMsecTimeout=600000

```
###💡 参数解释* **`-Dmapreduce.map.memory.mb=1024`**: 只申请 1GB 内存。因为数据才 7KB，给多了浪费。
* **`-w 3`**: 强行把这 7KB 数据切成 3 份给 3 个节点跑（虽然每个节点只分到 2KB 数据，但这正是验证“分布式”逻辑最好的微型实验）。
* **总资源消耗**: 4 个容器 x 1GB = **4GB**。这在你的集群里应该能轻松跑起来，秒级完成。


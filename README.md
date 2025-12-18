# 作业提交模板

```
.
├── code/                   # 所有实验代码
└── README.md               # 项目核心文档
```

## 研究目的
比较 Giraph 和 MapReduce 运行 PageRank 算法的差异

## 研究内容
1. 对比分析 Giraph 和 MapReduce 在执行 PageRank 算法等图迭代计算任务时的差异。
2. 深入理解 Giraph 所采用的 BSP (Bulk Synchronous Parallel) 设计理念及其在图计算中的优势。
3. 重点探讨两者在数据通信方式、任务调度机制及迭代开销等方面的不同，以及这些差异对算法性能与可扩展性的影响。
**增加的研究内容：**

## 实验
1. 分别基于Giraph和MapReduce实现PageRank算法，从编程模型角度分析两种系统在算法表达与数据流处理方式上的差异。
2. 在不同规模的图数据集上运行实验，记录作业执行时间、网络通信量以及内存占用等指标，对比分析Giraph与MapReduce在图迭代计算中的性能表现。

### 实验环境
#### 硬件配置
本次实验部署在分布式集群上，包含 **1 个主节点** 和 **3 个子节点**，满足节点数 (>=3) 的要求。
* **节点拓扑**：
    * **Master**: `ecnu01` (NameNode, ResourceManager)
    * **Slaves**: `ecnu02`, `ecnu03`, `ecnu04` (DataNode, NodeManager)
* **单节点配置**：
    * **CPU**: 4 核 (vCPU)
    * **内存**: 8 GiB
    * **操作系统**: Ubuntu 24.04 64位
    * **网络带宽**: 100 Mbps (峰值)
    * **区域**: 华东2 (上海) 可用区 B

#### 软件配置
* **操作系统**：Linux
* **JDK 版本**：Java 8 OpenJDK
* **Hadoop 版本**：2.7.3 (HDFS + YARN)
* **Giraph 版本**：1.3.0-SNAPSHOT (针对 Hadoop 2.7.3 重新编译，已解决 Protobuf 2.5.0 冲突)
* **Build Tool**：Maven 3.x

### 实验负载
本次实验使用 PageRank 算法作为核心工作负载，测试数据集分为两组：
1.  **逻辑验证数据集 (`init_graph.txt`)**
    * **规模**：小规模手动构建数据（约 10 个节点）。
    * **格式**：Tab 分隔文本 (TSV)，`NodeID [TAB] PR_Value [TAB] OutLink1,OutLink2...`。
    * **用途**：用于验证 MapReduce 链式作业的迭代逻辑正确性及收敛性。
2.  **性能测试数据集 (`input_json.txt`)**
    * **规模**：中等规模图数据。
    * **格式**：JSON 格式，`[顶点ID, 顶点值, [[目标顶点ID1, 边权重1], ...]]`。
    * **用途**：作为 Giraph 框架的标准输入，同时用于对比两种框架在大规模迭代下的运行时长。

### 实验步骤
列出执行实验的关键步骤，并对关键步骤进行截图，如 MapReduce / Spark / Flink 部署成功后的进程信息、作业执行成功的信息等，**截图能够通过显示用户账号等个性化信息佐证实验的真实性**。
#### 一、 环境与服务检查

在 Master 节点 (`ecnu01`) 检查 HDFS 和 YARN 服务状态，确保所有 Slave 节点正常在线。

- 命令：`jps`
- 预期：Master 节点包含 `ResourceManager`, `NameNode`；Slave 节点包含 `NodeManager`, `DataNode`。

*(此处待补充：Master 和 Slave 节点的 jps 运行截图)*

#### 实验设计与执行策略

本实验旨在多维度评估 Giraph (BSP) 与 MapReduce 在不同资源环境下的行为差异。为了全面捕捉性能特征，我们设计了 **3×3×2** 的实验矩阵（3种调度器 × 3种数据集 × 2种框架），并针对 FIFO 调度器增加了特定的并发阻塞测试。

**1. 为什么要测试三种调度器？**
* **FIFO (先进先出)**：作为基准对照，验证在大作业独占资源时，BSP 框架是否会加剧集群的“排头阻塞”效应。
* **Capacity (容量调度)**：模拟 Hadoop 生产环境的默认配置，重点考察在资源受限（Container 额度固定）时，Giraph 的“群组调度”机制是否容易引发资源死锁。
* **Fair (公平调度)**：探究在多任务竞争环境下，动态资源分配能否缓解 Giraph 的长尾效应（Straggler）及内存碎片问题。

**2. 为什么要进行“并发阻塞”测试？**
单纯的单任务运行无法反映分布式系统的真实负载。通过“先提交大作业，后提交小作业”的测试，可以直观地证明不同调度器对作业等待时间（Wait Time）的影响，特别是验证 Giraph 这种长期占用容器的框架对后续 MR 短作业的阻塞程度。

---

#### 关键实验步骤

实验分为环境准备、MapReduce 基准测试、Giraph 深度测试（含参数调优与死锁排查）、以及调度器特性测试四个阶段。

**第一阶段：环境与服务检查**

在所有实验开始前，必须在 Master 节点 (`ecnu01`) 检查 HDFS 和 YARN 服务状态，确保所有 Slave 节点正常在线，避免因节点掉线导致的实验误差。

* **命令**：`jps`
* **预期结果**：
    * Master 节点包含：`ResourceManager`, `NameNode`, `JobHistoryServer`
    * Slave 节点包含：`NodeManager`, `DataNode`

![Master 和 Slave 节点的 jps 运行截图]

**第二阶段：环境监控部署**

为了获取秒级的性能波动数据（用于生成波形图），需在 Slave 节点部署监控工具。

1.  **开启 History Server**：在 Master 节点执行 `mr-jobhistory-daemon.sh start historyserver`，确保所有作业的 Counter 指标（如 CPU Time, Bytes Read）可追溯。
2.  **部署 dstat**：在 Slave 节点（如 `ecnu03`）运行以下命令，采集 CPU 脉冲（验证 BSP 同步）与磁盘 I/O（验证 MR Shuffle）。
    ```bash
    dstat -tcmnd --output [Dataset]_[Scheduler]_[Framework].csv 1
    ```

**第三阶段：MapReduce 性能测试 (Baseline)**

针对 Small (`random_100`), Medium (`web-Stanford`), Large (`roadNet-CA`) 三个数据集，分别提交 MapReduce 作业。

1.  **启动监控**：在 Slave 节点启动 `dstat`。
2.  **清理环境**：每次运行前执行 `hdfs dfs -rm -r /giraph/output_mr` 确保输出目录净空。
3.  **提交作业与参数配置**：
    为了保证性能可比性并防止默认内存过小导致频繁 GC，我们显式设置了内存参数。
    * **通用参数配置**：
        * `mapreduce.job.reduces`: 3
        * `mapreduce.map.memory.mb`: 3072 (3GB)
        * `mapreduce.map.java.opts`: -Xmx2560m
        * `mapreduce.reduce.memory.mb`: 3072 (3GB)
        * `mapreduce.reduce.java.opts`: -Xmx2560m
        * `mapreduce.task.io.sort.mb`: 512
4.  **停止监控**：作业 Success 后停止 `dstat` 并保存生成的 CSV 文件。

**第四阶段：Giraph 性能测试与调优 (Core)**

Giraph 对内存和容器数量极其敏感。在实验过程中，针对不同规模的数据集，我们采取了不同的运行策略以解决资源死锁与 OOM 问题。

**1. 小数据集 (random_100) 的微型分布式验证**
* **目的**：验证即使是只有 7KB 的微小数据，Giraph 也能通过多 Worker 运行（验证分布式通信逻辑），但会观察到较大的系统协调开销。
* **配置策略**：使用最小化内存配置，避免占用过多资源。
* **命令参数**：
    * **Worker 数量**：`-w 3`
    * **内存**：`-Dmapreduce.map.memory.mb=1024` (1GB)
    * **Master/Worker 分离**：`-ca giraph.SplitMasterWorker=true`
    * **JVM Heap**：`-Dmapreduce.map.java.opts=-Xmx900m`

**2. 中数据集 (web-Stanford) 的标准测试**
* **配置**：使用标准分布式配置。
    * **Worker 数量**：`-w 3`
    * **Master/Worker 分离**：`-ca giraph.SplitMasterWorker=true`
* **目的**：验证 BSP 模型在多节点间的同步屏障特征（网络脉冲）。

**3. 大数据集 (roadNet-CA) 的死锁排查与终极调优**
在 Capacity 和 Fair 调度器下运行大图时，遭遇了严重的资源瓶颈，排查过程如下：

* **故障 A (OOM)**：
    * **现象**：使用默认内存参数时，日志报错 `OutOfMemoryError: Java heap space`。
    * **分析**：RoadNet 需加载全量图数据进内存，默认 1GB Container 无法承载。
    * **修正**：增加内存参数至 `-Dmapreduce.map.memory.mb=4096`。

* **故障 B (资源死锁 Resource Deadlock)**：
    * **现象**：增加内存后，使用 `-w 3` 提交，任务卡在 map 25% (或 67%) 进度不动。查看日志发现 `Headroom: <memory:0>`。
    * **分析**：YARN 集群总资源有限。Master 容器启动后占用了资源，导致剩余资源不足以启动所需的 3 个 Worker。Master 等 Worker 启动，Worker 等资源释放，形成循环等待（死锁）。

* **最终方案 (Final Solution) —— 单兵作战策略**：
    * **原理**：将作业原子需求降低为 **1 个容器**。只要能申请到一个容器，Master 和 Worker 就在同一进程内运行，彻底规避了分布式资源死锁。
    * **最终命令配置**：
        * **内存**：`-Dmapreduce.map.memory.mb=4096` (4GB)
        * **Worker 数量**：`-w 1`
        * **Master/Worker 合并**：`-ca giraph.SplitMasterWorker=false` (关键参数)

**第五阶段：调度器特性测试**

**1. 切换调度器**
1.  修改 `yarn-site.xml` 中的 `yarn.resourcemanager.scheduler.class` 属性（分别设置为 `FifoScheduler`, `CapacityScheduler`, `FairScheduler`）。
2.  使用 `scp` 分发配置文件至所有 Slave 节点。
3.  重启 YARN 服务：`stop-yarn.sh` -> `start-yarn.sh`。

**2. 执行并发阻塞测试 (仅 FIFO 模式)**
* **步骤**：
    1.  开启两个终端窗口。
    2.  **窗口 A**：提交一个长耗时的 Giraph 大作业（配置参考第四阶段的大数据集最终方案）。
    3.  **窗口 B**：等待约 10 秒后，立即提交一个 MapReduce 小作业。
* **观测点**：
    1.  刷新 YARN Web UI (`http://MasterIP:8088`)。
    2.  **关键现象**：记录 MapReduce 任务状态长时间处于 `ACCEPTED`（而非 `RUNNING`），且进度条停滞，直到 Giraph 任务彻底完成后，MR 任务才瞬间开始执行。
    3.  **取证**：对包含两个任务状态的界面进行截图。
* **结论**：验证了 FIFO 调度器缺乏资源抢占机制，存在严重的排头阻塞问题。

#### 四、 Giraph PageRank 实验

### 实验结果与分析

本节基于 **Capacity 调度器** 环境，对比了 MapReduce 与 Giraph 在处理相同数据集 (`stanford_input`) 时的性能表现。我们通过 `dstat` 采集了全过程的 CPU、内存、网络及磁盘指标。

##### 1. 核心指标对比汇总

下表展示了两次实验的关键性能指标对比。

| 监控维度 | 关键指标 | **Giraph (BSP模型)** | **MapReduce (传统模型)** | **对比结论** |
| --- | --- | --- | --- | --- |
| **作业效率** | **运行总耗时** | **42 秒** | **591 秒** | **Giraph 快 14 倍**，完全碾压 MapReduce。 |
| **资源消耗** | **CPU 利用率 (Avg)** | **44.9%** (User+Sys) | **37.1%** (User+Sys) | Giraph 计算密度更高，CPU 一直在有效工作；MR 存在等待间隙。 |
| **通信开销** | **网络吞吐峰值** | **44.0 MB/s** | **89.6 MB/s** | MR 的 Shuffle 阶段引发了比 Giraph 更猛烈的网络风暴。 |
| **内存占用** | **内存峰值** | **1.51 GB** | **1.54 GB** | 两者内存占用相当（受限于小数据规模，未触及内存瓶颈）。 |
| **I/O 特征** | **磁盘读写** | **~0 MB/s** | **~0 MB/s*** (见下方分析) | 在小数据下，两者物理磁盘 I/O 均不明显。 |

##### 2. 深入图表分析

###### (1) 网络通信模式对比：BSP 脉冲 vs Shuffle 风暴

- **Giraph (图A)**：网络流量呈现出清晰的 **“周期性脉冲”** 特征。每一个波峰对应一个 Superstep 的结束，验证了 BSP 模型中 **“计算 -> 同步(发消息) -> 计算”** 的执行节奏。
- **MapReduce (图B)**：网络流量呈现 **“集中爆发”** 特征（峰值高达 89.6 MB/s）。这是典型的 **Shuffle 阶段** 行为，所有节点同时在网络上拉取数据，极易造成网络拥塞。

###### (2) CPU 负载特征分析

- **Giraph**：CPU 曲线较为平稳且持续处于中高位（45%左右），说明计算节点一直在进行图的迭代计算，没有明显的阻塞。
- **MapReduce**：CPU 曲线存在波动。虽然平均利用率较低（37%），但这是因为 CPU 经常需要等待数据传输（Shuffle）或序列化操作，导致计算资源利用不充分。

###### (3) 关于“磁盘 I/O 为零”的特别说明

在本次实验中，我们观测到 MapReduce 的物理磁盘 I/O 极低。这**并非**意味着 MapReduce 不读写磁盘，而是因为：

- **Linux Page Cache 机制**：本次实验数据集仅 38MB，远小于节点内存（8GB）。操作系统自动将所有中间文件缓存在了 RAM 中，导致物理磁盘读写被“屏蔽”。
- **推论**：如果数据量增加到 10GB 以上（超过内存容量），MapReduce 的磁盘 I/O 曲线将会剧烈飙升，而 Giraph 将因内存溢出而失败或性能急剧下降。

### 实验结论

1. **性能差异**：在迭代图计算场景下，Giraph 凭借 **常驻内存 (In-Memory)** 和 **BSP 消息传递** 机制，比基于磁盘 Shuffle 的 MapReduce 快了一个数量级（14x）。
2. **调度影响**：在 Capacity 调度下，两者均能获得稳定的资源，但 MapReduce 对网络带宽的瞬时压力更大，更容易干扰集群中的其他作业。
3. **适用场景**：
    - **Giraph** 适合处理 **“存得下”** 的图数据，追求极致速度。
    - **MapReduce** 适合处理 **“超大规模”**（远超内存）的数据清洗和一次性计算任务，但不适合迭代算法。

### 结论

总结研究的主要发现。

### 分工

尽可能详细地写出每个人的具体工作和贡献度，并按贡献度大小进行排序。

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
* 命令：`jps`
* 预期：Master 节点包含 `ResourceManager`, `NameNode`；Slave 节点包含 `NodeManager`, `DataNode`。

*(此处待补充：Master 和 Slave 节点的 jps 运行截图)*

#### 二、 MapReduce PageRank 实验
**1. 算法编译与打包**
在本地开发环境使用 Maven 编译 MapReduce 实现代码，生成 Jar 包。
```bash
mvn clean package
```

### 实验结果与分析
使用表格和图表直观呈现结果，并解释结果背后的原因。

### 结论
总结研究的主要发现。

### 分工
尽可能详细地写出每个人的具体工作和贡献度，并按贡献度大小进行排序。

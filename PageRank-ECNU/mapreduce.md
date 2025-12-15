## 🛠️ MapReduce PageRank 详细实现方案

### 1. 核心挑战与解决方案

在 MapReduce 框架下实现图算法面临的最大挑战是 **"无状态性" (Statelessness)**。每次 MapReduce 作业结束后，内存中的数据会被清空，这意味着必须将图的完整拓扑结构（谁指向谁）写入磁盘，以便传递给下一次迭代。

我们通过设计特殊的 **Key-Value 传输协议** 解决了这个问题：

### A. Mapper 阶段的设计 (双重分发)

Mapper 处理每一行输入 `NodeID \\t CurrentPR \\t Target1,Target2...` 时，执行两个核心动作：

1. **分发权重 (Vote)**: 计算当前节点给每个邻居的贡献值 `PR / OutDegree`，并向目标节点发送。
2. **传递图结构 (Topology Preservation)**: 为了防止图结构在 Shuffle 阶段丢失，Mapper 会将原始的链接关系以特殊前缀 `GRAPH_STR:` 标记，并发回给自己（Key 为当前 NodeID）。
    - *代码逻辑*: `context.write(new Text(nodeId), new Text("GRAPH_STR:" + parts[2]));`

### B. Reducer 阶段的设计 (结构重组)

Reducer 会收到同一个 NodeID 的一系列 Value，其中既包含邻居发来的"投票分值"，也包含 Mapper 传回的"图结构"。

1. **数据分离**: 遍历 Values，通过检查字符串前缀是否为 `GRAPH_STR:` 来区分这是图结构数据还是投票数值。
2. **PageRank 计算**: 累加所有投票数值得到 `Sum(InComingVotes)`，应用公式 `NewPR = (1-d) + d * Sum`。
3. **格式对齐**: 将计算出的 `NewPR` 与解析出的 `TargetNodes` 重新拼接，输出为与输入格式完全一致的 `Key \\t Value`，供下一轮迭代读取。

### 2. 迭代控制 (Driver 实现)

由于 MapReduce 原生不支持循环，我们在 Driver 端采用 **链式作业 (Chained Jobs)** 机制：

- **动态路径**: 使用 `for` 循环控制迭代次数（默认 10 次）。
- **输入输出流转**:
    - 第 1 次迭代读取初始路径。
    - 第 N 次迭代读取第 N-1 次迭代的输出目录。
    - *逻辑实现*: `String currentInput = (i == 0) ? inputPath : basePath + i;`
- **自动清理**: 每次启动作业前，Driver 会调用 HDFS API 检查并删除已存在的输出目录，防止 Hadoop 抛出 `FileAlreadyExistsException` 异常。

### 3. 数据格式规范

为了降低解析开销，本项目未使用 XML 或 JSON，而是采用了轻量级的 **Tab 分隔符 (TSV)** 格式：

- **Input/Output**: `NodeID [TAB] PR_Value [TAB] OutLink1,OutLink2,OutLink3`
- **优势**: 相比 JSON，使用 `String.split("\\t")` 解析速度更快，更适合 MapReduce 这种 I/O 密集型任务。

### 4. 关键参数

- **Damping Factor (阻尼系数)**: `0.85`
- **Max Iterations (最大迭代)**: `10` (根据实验，小规模数据集通常在 10 轮内收敛)

## 🎓 附录：模拟答辩常见问题 (Q&A)

针对 MapReduce 跑 PageRank 这一经典大数据题目，以下是老师可能会问到的核心问题及标准回答思路。

### Q1: 为什么要在 Mapper 里把图结构（TargetNodes）再输出一遍？

**回答：**
这是由 MapReduce 的 **无状态 (Stateless)** 特性决定的。

- Reduce 阶段的任务是计算当前轮次新的 PR 值。
- 如果 Mapper 不把“图的拓扑结构”（即我指向了谁）传递给 Reducer，Reducer 虽然能算出新的分值，但无法将这个分值与链接关系重新拼接。
- **后果**：下一轮迭代将丢失图的连线信息，计算链条会因此中断。

---

### Q2: 你们的算法怎么判断“收敛” (Convergence)？

**回答：**
在目前的实现中，为了简化逻辑，我们采用了 **固定迭代次数（10次）** 的方式。根据实验，小规模数据通常在 10 轮内即可达到稳定。

> 🌟 进阶回答（加分项）：
如果要更严谨，我们可以在 Driver 端利用 Hadoop 的 Counter（全局计数器）。
> 
> - 在 Reducer 中计算 `abs(OldPR - NewPR)`，并将差值累加到全局 Counter 中。
> - 在 Driver 中监控该 Counter，如果一轮结束后的总偏差小于设定的阈值（如 0.0001），则提前 `break` 循环终止迭代。

---

### Q3: 既然 MapReduce 这么麻烦（要频繁读写磁盘），为什么还要用它？

**回答：**
虽然 MapReduce 在迭代计算上效率较低，但它有两个不可替代的优势：

1. **极强的容错性**：MapReduce 的中间结果都持久化到磁盘。如果计算过程中某台机器宕机，只需要重跑该节点上的任务（Task）即可，无需回滚整个作业。而基于内存的 Giraph 如果节点挂掉，恢复成本通常更高（可能需要回滚到上一个 Checkpoint）。
2. **学习基石**：理解 MapReduce 的 Shuffle 过程是理解分布式计算（包括 Spark、Flink）数据流转的基础。

---

### Q4: 如果遇到“死胡同”节点（Dangling Node，没有出链的网页）怎么办？

**回答：**
在目前的简化代码实现中，如果 `targets.length == 0`，Mapper 就不会分发任何分数。

- **现象**：这会导致全图的 PR 总分在迭代过程中慢慢“泄漏”，总和会小于 1。
- **标准解法**：在工业级实现中，应该识别这些节点，将其 PR 值累加到一个全局变量中，然后在每一轮结束时，将这部分“泄漏”的分数平均补偿给全图所有节点。本次实验主要关注框架性能对比，因此对算法细节做了适当简化。

---

### Q5: 你们对比了 MapReduce 和 Giraph，最大的性能差距来源是什么？

**回答：**
根本原因在于 **I/O 开销**。

- **MapReduce**：运行 10 轮迭代，实际上执行了 10 次完整的 `读取 HDFS -> 序列化 -> 网络 Shuffle -> 写入 HDFS` 流程。磁盘 I/O 占据了绝大部分时间。
- **Giraph**：采用 BSP 模型，图数据加载一次后常驻内存。10 轮迭代仅仅是内存中的状态更新和网络消息传递，完全省去了中间 9 次的磁盘读写，因此在运行效率上具有数量级的优势。
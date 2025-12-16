# Hadoop+Giraph环境使用说明文档

## 1. 系统环境概述

### 1.1 集群配置
本环境配置了一个分布式Hadoop+Giraph集群，包含1个主节点和3个子节点：

**主节点（ecnu01）配置**：
- **操作系统**：Linux
- **Java环境**：Java 8 OpenJDK
- **Hadoop版本**：2.7.3（已配置HDFS和YARN）
- **Giraph版本**：1.3.0-SNAPSHOT（已适配Hadoop 2.7.3）

**子节点（ecnu02、ecnu03、ecnu04）配置**：
- **操作系统**：Linux
- **Java环境**：Java 8 OpenJDK
- **Hadoop组件**：DataNode、NodeManager
- **功能**：数据存储和计算任务执行

### 1.2 服务状态

**主节点（ecnu01）运行的服务**：
- **NameNode**：HDFS主节点
- **SecondaryNameNode**：HDFS辅助节点
- **ResourceManager**：YARN资源管理
- **DataNode**：本地HDFS数据节点
- **NodeManager**：本地YARN节点管理

**子节点（ecnu02、ecnu03、ecnu04）运行的服务**：
- **DataNode**：HDFS数据节点
- **NodeManager**：YARN节点管理

## 2. 主机连接说明

### 2.1 连接方式

**连接主节点**：
```bash
ssh root@ecnu01
# 或使用IP地址
ssh root@106.15.248.68
# 输入密码
```
四台主机的密码都是Xuchen666
**连接子节点**：
```bash
# 连接子节点1
ssh root@ecnu02
ssh root@139.196.240.167
# 连接子节点2
ssh root@ecnu03
ssh root@139.224.227.56
# 连接子节点3
ssh root@ecnu04
ssh root@47.116.114.140
```

### 2.2 工作目录

**主节点（ecnu01）工作目录**：
- **Giraph源码目录**：`/root/giraph/giraph`
- **Giraph编译输出**：`/root/giraph/giraph/giraph-examples/target/`
- **测试数据目录**：`/root/giraph/`
- **Hadoop安装目录**：`/usr/local/hadoop/`

**子节点（ecnu02、ecnu03、ecnu04）工作目录**：
- **Hadoop安装目录**：`/usr/local/hadoop/`
- **数据存储目录**：`/usr/local/hadoop/tmp/dfs/data`

## 3. Hadoop环境使用

### 3.1 检查Hadoop服务状态

```bash
# 检查所有Hadoop相关进程
jps

# 预期输出应包含：NameNode, SecondaryNameNode, DataNode, ResourceManager, NodeManager
```

### 3.2 启动/停止Hadoop服务

**在主节点（ecnu01）上操作**：

```bash
# 启动HDFS服务（主节点）
/usr/local/hadoop/sbin/hadoop-daemon.sh start namenode
/usr/local/hadoop/sbin/hadoop-daemon.sh start secondarynamenode

# 启动YARN服务（主节点）
/usr/local/hadoop/sbin/yarn-daemon.sh start resourcemanager

# 使用集群脚本同时启动所有节点服务
/usr/local/hadoop/sbin/start-dfs.sh  # 启动所有节点的HDFS服务
/usr/local/hadoop/sbin/start-yarn.sh  # 启动所有节点的YARN服务

# 使用集群脚本停止所有服务
/usr/local/hadoop/sbin/stop-dfs.sh   # 停止所有节点的HDFS服务
/usr/local/hadoop/sbin/stop-yarn.sh  # 停止所有节点的YARN服务

# 单独停止服务示例（将start替换为stop）
/usr/local/hadoop/sbin/hadoop-daemon.sh stop datanode
```

**在子节点上单独操作**：
```bash
# 启动DataNode服务
/usr/local/hadoop/sbin/hadoop-daemon.sh start datanode

# 启动NodeManager服务
/usr/local/hadoop/sbin/yarn-daemon.sh start nodemanager
```

### 3.3 HDFS操作命令

```bash
# HDFS文件系统操作
/usr/local/hadoop/bin/hdfs dfs -ls /path/to/directory  # 列出目录内容
/usr/local/hadoop/bin/hdfs dfs -mkdir -p /path/to/directory  # 创建目录
/usr/local/hadoop/bin/hdfs dfs -put local_file /hdfs/path  # 上传文件
/usr/local/hadoop/bin/hdfs dfs -get /hdfs/path local_path  # 下载文件
/usr/local/hadoop/bin/hdfs dfs -rm -r /hdfs/path  # 删除文件/目录
```

## 4. Giraph环境使用

### 4.1 Giraph配置说明

Giraph已成功编译，适配Hadoop 2.7.3版本，并解决了Protobuf版本冲突问题：

- 在`/root/giraph/giraph/pom.xml`中添加了`<protobuf.version>2.5.0</protobuf.version>`配置
- 编译生成的JAR包位于：`/root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar`

### 4.2 验证Giraph环境

可以通过运行测试程序验证Giraph库是否正常工作：

```bash
cd /root/giraph
java -cp .:/root/giraph/giraph/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.7.3-jar-with-dependencies.jar:/usr/local/hadoop/share/hadoop/common/*:/usr/local/hadoop/share/hadoop/hdfs/*:/usr/local/hadoop/share/hadoop/mapreduce/*:/usr/local/hadoop/share/hadoop/yarn/* GiraphTest
```

## 5. 数据准备

### 5.1 示例数据

系统中已准备好图数据示例：

- **JSON格式图数据**：`/root/giraph/input_json.txt`
  内容格式：`[顶点ID, 顶点值, [[目标顶点ID1, 边权重1], [目标顶点ID2, 边权重2], ...]]`
  
  示例内容：
  ```
  [1,0,[[2,1.0],[3,1.0]]]
  [2,0,[[3,1.0]]]
  [3,0,[[1,1.0]]]
  ```

### 5.2 数据上传到HDFS

```bash
# 创建HDFS目录
/usr/local/hadoop/bin/hdfs dfs -mkdir -p /giraph/input

# 上传数据
/usr/local/hadoop/bin/hdfs dfs -put /root/giraph/input_json.txt /giraph/input/

# 检查上传结果
/usr/local/hadoop/bin/hdfs dfs -ls /giraph/input/
```



### 9.2 Hadoop任务监控

- 通过Web界面访问：
  - HDFS管理界面：`http://服务器IP:50070`
  - YARN管理界面：`http://服务器IP:8088`

- 命令行监控：
  ```bash
  # 查看正在运行的YARN任务
  /usr/local/hadoop/bin/yarn application -list
  
  # 查看任务日志
  /usr/local/hadoop/bin/yarn logs -applicationId <ApplicationID>
  ```

## 10. 注意事项

1. **资源使用**：运行大规模图计算任务时，注意监控内存使用情况，避免内存溢出
2. **HDFS空间**：定期清理HDFS上的临时文件和旧的输出结果
3. **日志管理**：定期检查和清理各节点上的Hadoop和Giraph日志文件
4. **版本兼容性**：本环境已解决Giraph与Hadoop 2.7.3的Protobuf版本冲突，请勿随意修改版本配置
5. **集群同步**：确保所有节点的Hadoop配置文件保持同步
6. **网络连接**：确保节点间网络连接稳定，避免任务执行中断
7. **服务状态**：定期检查所有节点的DataNode和NodeManager服务状态

## 11. 网络配置与IP映射注意事项

### 11.1 内网IP映射说明

当前Hadoop集群配置使用了内网IP地址映射：
- 在所有节点的`/etc/hosts`文件中，主机名（ecnu01、ecnu02、ecnu03、ecnu04）被映射到特定的内网IP地址
- 这些IP地址可能会在网络环境变化或机器重启后发生变动

### 11.2 配置检查与修改

在不同网络环境或不同机器上使用此配置时，需要进行以下操作：

```bash
# 1. 检查当前网络环境中的IP地址
ifconfig
# 或
ip addr

# 2. 修改/etc/hosts文件，更新主机名与IP地址的映射
vim /etc/hosts

# 3. 更新所有节点的相同配置，确保主机名解析一致
```


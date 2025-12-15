import sys
import re
import math

def parse_duration(time_str):
    """
    将 Hadoop UI 的时间字符串 (e.g., "1min 30sec", "450ms", "12sec") 
    统一转换为秒 (float)。
    """
    # 移除多余空格并转小写
    s = time_str.strip().lower().replace(",", "")
    
    if not s:
        return None

    total_seconds = 0.0
    
    # 1. 处理毫秒 (ms)
    if 'ms' in s:
        ms_match = re.search(r'(\d+)\s*ms', s)
        if ms_match:
            total_seconds += float(ms_match.group(1)) / 1000.0
        return total_seconds

    # 2. 处理分钟 (min)
    min_match = re.search(r'(\d+)\s*min', s)
    if min_match:
        total_seconds += float(min_match.group(1)) * 60

    # 3. 处理秒 (sec / s)
    sec_match = re.search(r'(\d+)\s*(sec|s)', s)
    if sec_match:
        total_seconds += float(sec_match.group(1))
    
    # 4. 如果只有纯数字，默认当作秒
    if total_seconds == 0:
        try:
            val = float(s)
            return val
        except ValueError:
            return None # 无法解析的脏数据

    return total_seconds

def analyze_tasks(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
    except FileNotFoundError:
        print(f"错误: 找不到文件 '{file_path}'")
        return

    # 解析数据
    durations = []
    for line in lines:
        val = parse_duration(line)
        if val is not None:
            durations.append(val)

    count = len(durations)
    if count == 0:
        print("文件中没有有效的时间数据。")
        return

    # 计算统计指标
    avg_time = sum(durations) / count
    
    # 计算标准差 (Standard Deviation)
    variance = sum((x - avg_time) ** 2 for x in durations) / count
    std_dev = math.sqrt(variance)

    # 定义长尾任务 (Straggler): 超过平均值 1.5 倍的任务
    threshold = avg_time * 1.5
    stragglers = [x for x in durations if x > threshold]
    straggler_count = len(stragglers)
    straggler_percentage = (straggler_count / count) * 100

    # 找到最慢的任务
    max_time = max(durations)
    min_time = min(durations)

    # --- 输出报告 ---
    print("-" * 40)
    print(f"任务耗时分析报告: {file_path}")
    print("-" * 40)
    print(f"总任务数 (Total Tasks):      {count}")
    print(f"平均耗时 (Average Time):     {avg_time:.2f} s")
    print(f"标准差 (Std Dev):            {std_dev:.2f} s")
    print(f"最快任务 (Min Time):         {min_time:.2f} s")
    print(f"最慢任务 (Max Time):         {max_time:.2f} s")
    print("-" * 40)
    print(f"长尾阈值 (Avg * 1.5):      > {threshold:.2f} s")
    print(f"长尾任务数 (Stragglers):   {straggler_count}")
    print(f"长尾占比 (Percentage):     {straggler_percentage:.2f}%")
    print("-" * 40)
    
    # 简短结论建议
    print("\n[报告]:")
    if straggler_percentage > 5:
        print(f"  > 检测到明显的负载不均衡，长尾任务占比 {straggler_percentage:.1f}%。")
        print(f"  > 最慢任务耗时是最快任务的 {max_time/min_time:.1f} 倍，说明存在『木桶效应』。")
    else:
        print(f"  > 负载相对均衡，长尾任务较少。")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("使用方法: python analyze_stragglers.py <你的数据文件.txt>")
    else:
        analyze_tasks(sys.argv[1])
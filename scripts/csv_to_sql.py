#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CSV → SQL 转换脚本
将 CSV 文件转换为 MySQL 的 CREATE TABLE + INSERT 语句

用法:
    python csv_to_sql.py employee.csv        # 输出到终端
    python csv_to_sql.py employee.csv -t 员工表 # 指定表名
    python csv_to_sql.py employee.csv -o output.sql  # 输出到文件
"""

import csv
import argparse
import sys
import re
import os


def sanitize_column_name(name: str) -> str:
    """清洗列名为合法的 MySQL 列名（中文保留，去特殊符号）"""
    name = name.strip().strip('"').strip("'")
    return re.sub(r'[^a-zA-Z0-9_一-鿿]', '_', name)


def infer_mysql_type(values: list[str]) -> str:
    """根据列值推测 MySQL 数据类型"""
    non_empty = [v for v in values if v.strip()]
    if not non_empty:
        return 'VARCHAR(500)'

    # 检查是否全是整数
    if all(re.match(r'^-?\d+$', v.strip()) for v in non_empty):
        max_val = max(abs(int(v.strip())) for v in non_empty)
        if max_val <= 2147483647:
            return 'INT'
        return 'BIGINT'

    # 检查是否全是浮点数
    if all(re.match(r'^-?\d+\.?\d*$', v.strip()) for v in non_empty):
        return 'DOUBLE'

    # 检查最大长度
    max_len = max(len(v.strip()) for v in non_empty)
    if max_len <= 255:
        return f'VARCHAR({max(max_len * 2, 50)})'
    elif max_len <= 65535:
        return 'TEXT'
    return 'MEDIUMTEXT'


def escape_sql(value: str) -> str:
    """转义 SQL 字符串"""
    value = value.strip()
    if value.startswith('"') and value.endswith('"'):
        value = value[1:-1]
    if value.startswith("'") and value.endswith("'"):
        value = value[1:-1]
    value = value.replace('\\', '\\\\')
    value = value.replace("'", "\\'")
    return value


def csv_to_sql(csv_path: str, table_name: str = None, output_path: str = None,
               create_table: bool = True, batch_size: int = 500,
               encoding: str = 'utf-8'):
    """CSV → SQL 核心转换逻辑"""
    if table_name is None:
        base = os.path.splitext(os.path.basename(csv_path))[0]
        table_name = sanitize_column_name(base) or 'imported_table'

    # 先尝试常见编码
    rows = []
    headers = []
    for enc in [encoding, 'utf-8', 'gbk', 'gb2312', 'gb18030', 'latin-1']:
        try:
            with open(csv_path, 'r', encoding=enc) as f:
                # 嗅探 CSV 方言
                sample = f.read(8192)
                f.seek(0)
                dialect = csv.Sniffer().sniff(sample)
                reader = csv.reader(f, dialect)
                headers_raw = next(reader)
                headers = [sanitize_column_name(h) for h in headers_raw]
                for row in reader:
                    if row and any(c.strip() for c in row):
                        rows.append(row)
            encoding = enc
            break
        except (UnicodeDecodeError, csv.Error):
            continue
    else:
        raise ValueError(f"无法用任何编码读取文件: {encoding}, utf-8, gbk, gb2312, gb18030")

    if not rows:
        raise ValueError("CSV 文件为空或只有表头")

    print(f"✅ 检测编码: {encoding} | 列数: {len(headers)} | 行数: {len(rows)}", file=sys.stderr)

    # 推测每列类型
    columns_types = {}
    for i, header in enumerate(headers):
        col_values = [row[i] if i < len(row) else '' for row in rows[:200]]
        columns_types[header] = infer_mysql_type(col_values)

    lines = []
    lines.append('-- ================================')
    lines.append(f'-- 表: `{table_name}`')
    lines.append(f'-- 生成自: {os.path.basename(csv_path)}')
    lines.append(f'-- 列数: {len(headers)} | 行数: {len(rows)}')
    lines.append('-- ================================')

    # CREATE TABLE
    if create_table:
        lines.append(f'\nDROP TABLE IF EXISTS `{table_name}`;')
        lines.append(f'CREATE TABLE `{table_name}` (')
        col_defs = []
        for header in headers:
            col_type = columns_types[header]
            col_defs.append(f'  `{header}` {col_type} DEFAULT NULL')
        lines.append(',\n'.join(col_defs))
        lines.append(f') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;\n')

    # INSERT 语句（分批）
    for batch_start in range(0, len(rows), batch_size):
        batch_rows = rows[batch_start:batch_start + batch_size]
        lines.append(f'INSERT INTO `{table_name}` (`{"`, `".join(headers)}`) VALUES')

        value_lines = []
        for row in batch_rows:
            values = []
            for i, header in enumerate(headers):
                if i < len(row) and row[i].strip():
                    values.append(f"'{escape_sql(row[i])}'")
                else:
                    values.append('NULL')
            value_lines.append(f'  ({", ".join(values)})')

        lines.append(',\n'.join(value_lines) + ';\n')

    output = '\n'.join(lines)

    if output_path:
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(output)
        print(f'✅ 已输出: {output_path} ({len(output):,} 字符)', file=sys.stderr)
    else:
        print(output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='CSV → MySQL SQL 转换器')
    parser.add_argument('csv', help='CSV 文件路径')
    parser.add_argument('-t', '--table', help='目标表名（默认用文件名）')
    parser.add_argument('-o', '--output', help='输出 SQL 文件路径（默认输出到终端）')
    parser.add_argument('--no-create', action='store_true', help='不生成 CREATE TABLE')
    parser.add_argument('-b', '--batch', type=int, default=500, help='每批 INSERT 行数（默认 500）')
    parser.add_argument('-e', '--encoding', default='utf-8', help='首选编码（默认 utf-8）')
    args = parser.parse_args()

    try:
        csv_to_sql(
            csv_path=args.csv,
            table_name=args.table,
            output_path=args.output,
            create_table=not args.no_create,
            batch_size=args.batch,
            encoding=args.encoding,
        )
    except Exception as e:
        print(f'❌ 错误: {e}', file=sys.stderr)
        sys.exit(1)

#!/usr/bin/env python3
"""
JMH Benchmark Comparison Tool

Compares two JMH JSON result files and displays performance differences.

Usage:
    python3 compare.py <baseline.json> <optimized.json> [title]

Example:
    python3 compare.py baseline-quick.json phase1-simple.json "Phase 1 Results"
"""
import json
import sys

if len(sys.argv) < 3:
    print(__doc__)
    sys.exit(1)

baseline_file = sys.argv[1]
optimized_file = sys.argv[2]
title = sys.argv[3] if len(sys.argv) > 3 else "Performance Comparison"

with open(baseline_file) as f:
    baseline = json.load(f)
with open(optimized_file) as f:
    optimized = json.load(f)

# Create maps for easier lookup
baseline_map = {b['benchmark']: b for b in baseline}
optimized_map = {p['benchmark']: p for p in optimized}

print("=" * 80)
print(f"{title}")
print("=" * 80)
print()

for bench_name in sorted(optimized_map.keys()):
    if bench_name not in baseline_map:
        continue

    b = baseline_map[bench_name]
    p = optimized_map[bench_name]
    
    # Extract score
    b_score = b['primaryMetric']['score']
    p_score = p['primaryMetric']['score']
    
    # Calculate improvement
    if b['mode'] in ['thrpt', 'sample']:  # Higher is better
        improvement = ((p_score - b_score) / b_score) * 100
        direction = "↑" if improvement > 0 else "↓"
    else:  # avgt, ss - lower is better
        improvement = ((b_score - p_score) / b_score) * 100
        direction = "↓" if improvement > 0 else "↑"
    
    # Format scores
    unit = b['primaryMetric']['scoreUnit']
    
    bench_short = bench_name.split('.')[-1]
    params = b.get('params', {})
    param_str = f"({params})" if params else ""
    
    print(f"{bench_short:50s} {param_str:15s}")
    print(f"  Baseline:  {b_score:15.3f} {unit}")
    print(f"  Optimized: {p_score:15.3f} {unit}")
    print(f"  Change:    {direction} {abs(improvement):6.2f}%")
    print()


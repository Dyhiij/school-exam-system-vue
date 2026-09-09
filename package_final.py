"""打包学校管理系统为可移植版本"""
import zipfile
from pathlib import Path
from datetime import datetime
import os

os.chdir(r"c:\Users\34386\Desktop\学校管理\学校管理")

ROOT = Path(".")
NOW = datetime.now().strftime("%Y%m%d_%H%M%S")
OUTPUT = Path(r"c:\Users\34386\Desktop\学校管理") / f"学校管理系统_便携版_{NOW}.zip"

EXCLUDE = [
    ".vscode", ".git", ".idea", "__pycache__", "logs", "temp",
    "ib_logfile", "ibdata", "undo_", ".err", "auto.cnf", ".pid",
    "target/classes", "target/generated-sources", "target/maven-status",
    "target/test-classes", "dist", ".DS_Store", "Thumbs.db",
]


def should_exclude(rel):
    rp = rel.replace("\\", "/")
    for pat in EXCLUDE:
        if pat.replace("\\", "/") in rp:
            return True
    if rp.endswith(".log"):
        return True
    return False


def fmt_size(n):
    if n < 1024:
        return f"{n}B"
    if n < 1024 * 1024:
        return f"{n/1024:.1f}KB"
    if n < 1024 * 1024 * 1024:
        return f"{n/1024/1024:.1f}MB"
    return f"{n/1024/1024/1024:.2f}GB"


print(f"源目录: {ROOT.absolute()}")
print(f"输出: {OUTPUT.name}")
print()

# 收集文件
print("收集文件中...")
files = []
for p in ROOT.rglob("*"):
    if not p.is_file():
        continue
    rel = str(p.relative_to(ROOT))
    if should_exclude(rel):
        continue
    files.append((p, rel))

total_size = sum(p.stat().st_size for p, _ in files)
print(f"文件数: {len(files)}, 总大小: {fmt_size(total_size)}")
print()

# 压缩
print("开始压缩...")
written = 0
with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
    for p, rel in files:
        try:
            zf.write(str(p), arcname=rel)
            written += 1
            if written % 500 == 0:
                pct = int(written * 100 / len(files))
                print(f"  {pct:3d}% ({written}/{len(files)})")
        except Exception as e:
            print(f"  跳过 {rel}: {e}")

final_size = OUTPUT.stat().st_size
print()
print("=" * 60)
print("打包完成！")
print(f"  源文件数:   {len(files)}")
print(f"  源总大小:   {fmt_size(total_size)}")
print(f"  压缩包:     {OUTPUT.name}")
print(f"  压缩大小:   {fmt_size(final_size)}")
print(f"  压缩比:     {final_size*100/max(total_size,1):.1f}%")
print(f"  路径:       {OUTPUT}")
print("=" * 60)
print()
print("解压后双击「启动系统.bat」即可运行！")
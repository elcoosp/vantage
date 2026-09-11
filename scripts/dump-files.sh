#!/bin/bash
# Usage: ./dump-for-llm.sh <directory> [output-file]
# Bundles path + content of every non-gitignored text file, suitable for LLM ingestion.

set -euo pipefail

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Usage: $0 <directory> [output-file]" >&2
    exit 1
fi

target_dir="$1"
output_file="${2:-dump.txt}"

[ -d "$target_dir" ] || { echo "Error: '$target_dir' is not a directory." >&2; exit 1; }
cd "$target_dir"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 \
    || { echo "Error: '$target_dir' is not inside a Git repository." >&2; exit 1; }

# Resolve the output path absolutely so we can reliably skip it below.
mkdir -p "$(dirname "$output_file")"
abs_output="$(cd "$(dirname "$output_file")" && pwd)/$(basename "$output_file")"

# Tracked + untracked-but-not-ignored files, NUL-delimited to survive odd names.
git ls-files --cached --others --exclude-standard -z \
| while IFS= read -r -d '' file; do
    # Skip the dump file itself (prevents recursive self-inclusion).
    if [ "$(pwd)/$file" = "$abs_output" ]; then
        continue
    fi

    # Skip non-regular files (symlinks, dirs).
    if [ ! -f "$file" ]; then
        continue
    fi

    # Skip binary files. `file --mime-encoding` returns "binary" for non-text.
    encoding=$(file -b --mime-encoding "$file" 2>/dev/null || echo binary)
    if [ "$encoding" = "binary" ]; then
        continue
    fi

    echo "===== FILE: $file ====="
    cat "$file"
    echo
done > "$output_file"

echo "Wrote $output_file ($(wc -l < "$output_file") lines, $(du -h "$output_file" | cut -f1))"

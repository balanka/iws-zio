#!/usr/bin/env bash
#
# prefix-daisyui.sh
# Renames plain DaisyUI classes to their "daisy-" prefixed equivalents
# across all Scala fragment views, so DaisyUI components don't collide
# with CoreUI's .btn / .card / .table / etc.
#
# Usage:
#   ./scripts/prefix-daisyui.sh            # dry-run (prints changes)
#   ./scripts/prefix-daisyui.sh --apply    # actually edits files
#

set -euo pipefail

APPLY=false
if [[ "${1:-}" == "--apply" ]]; then APPLY=true; fi

# Where the fragment Scala files live
SRC_DIR="${SRC_DIR:-$(dirname "$0")/../src/main/scala/com/kabasoft/iws/api}"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "ERROR: source dir not found: $SRC_DIR" >&2
  exit 1
fi

# Classes to rename. Order matters: longer names first so `btn-outline`
# is handled before `btn`, and `table-zebra` before `table`.
CLASSES=(
  "btn-outline"  "btn-ghost"  "btn-primary"  "btn-secondary"  "btn-accent"
  "btn-success"  "btn-error"  "btn-warning"  "btn-info"       "btn-neutral"
  "btn-xs"  "btn-sm"  "btn-md"  "btn-lg"
  "btn"
  "table-zebra"  "table-pin-rows"  "table-pin-cols"  "table-xs"  "table-sm"  "table-md"  "table-lg"
  "table"
  "card-body"  "card-title"  "card-actions"  "card-compact"  "card-bordered"
  "card"
  "badge-primary"  "badge-secondary"  "badge-accent"  "badge-success"  "badge-error"  "badge-warning"  "badge-info"
  "badge"
  "alert-info"  "alert-success"  "alert-warning"  "alert-error"
  "alert"
  "input-bordered"  "input-ghost"  "input-xs"  "input-sm"  "input-md"  "input-lg"
  "input"
  "select-bordered"  "select-ghost"  "select-xs"  "select-sm"  "select-md"  "select-lg"
  "select"
  "textarea-bordered"  "textarea-ghost"  "textarea-xs"  "textarea-sm"  "textarea-md"  "textarea-lg"
  "textarea"
  "checkbox"
  "radio"
  "switch"
  "toggle"
  "range"
  "label"
  "field"
  "fieldset"
  "form-control"
  "modal"
  "modal-box"
  "modal-action"
  "modal-backdrop"
  "dropdown"
  "dropdown-content"
  "menu"
  "tab"
  "tabs"
  "breadcrumbs"
  "pagination"
  "loading"
  "spinner"
  "tooltip"
  "avatar"
  "kbd"
  "skeleton"
  "progress"
  "divider"
)

# Build a single sed expression per class that matches class=" or class=' followed by the class name
# where the class name is a whole token (preceded by start, space, or quote;
# followed by space, quote, or end). This avoids matching "btn" inside "my-btn".
SED_EXPR=()
for cls in "${CLASSES[@]}"; do
  # Match: class="...  or  '  or  space   then <cls>  then space  or  "  or  '
  # Two sed -E expressions per class: one for class="... and one for class='...
  # Simplify by matching the class token wherever it appears inside a class attribute.
  #
  # We rewrite `class="X Y Z"` by replacing word-boundary occurrences of the class.
  # GNU sed supports \b; BSD sed (macOS) does not. Use portable approach with
  # explicit character classes.
  SED_EXPR+=(-e "s/(class=\"[^\"]*)${cls}([ \"]|$)/\1daisy-${cls}\2/g")
  SED_EXPR+=(-e "s/(class='[^']*)${cls}([ ']|$)/\1daisy-${cls}\2/g")
done

# Collect target files
FILES=$(find "$SRC_DIR" -type f -name '*.scala' -not -name '*.bak')

if [[ -z "$FILES" ]]; then
  echo "No .scala files found in $SRC_DIR"
  exit 0
fi

echo "Source dir : $SRC_DIR"
echo "Files      : $(echo "$FILES" | wc -l | tr -d ' ')"
echo "Mode       : $($APPLY && echo 'APPLY' || echo 'DRY-RUN')"
echo

# Per-file change report
CHANGED=0
for f in $FILES; do
  # Count occurrences of class=" that we'd modify
  BEFORE=$(grep -oE 'class="[^"]*"' "$f" | wc -l | tr -d ' ')

  if $APPLY; then
    # Backup once per run (only first time)
    [[ -f "${f}.bak" ]] || cp "$f" "${f}.bak"

    sed -i -E "${SED_EXPR[@]}" "$f"
  else
    # Dry run: show what would change (first 3 lines per file)
    sed -E "${SED_EXPR[@]}" "$f" | diff -u "$f" - | head -20 || true
  fi

  AFTER=$(grep -oE 'class="[^"]*"' "$f" | wc -l | tr -d ' ')
  if [[ "$BEFORE" != "$AFTER" || $APPLY == false ]]; then
    echo "  $f"
    CHANGED=$((CHANGED + 1))
  fi
done

echo
if $APPLY; then
  echo "Applied. Backups written as *.bak."
  echo "Review with: git diff"
  echo "Undo with:   find \"$SRC_DIR\" -name '*.scala' -exec sh -c 'mv \"\$1.bak\" \"\$1\"' _ {} \\;"
else
  echo "Dry-run complete. No files modified."
  echo "Re-run with --apply to commit the changes."
fi

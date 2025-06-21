#!/usr/bin/env python3
"""Initialize the template by replacing placeholder values."""

import re
import shutil
from pathlib import Path

COLOR_CYAN = "\033[96m"
COLOR_GREEN = "\033[92m"
COLOR_RESET = "\033[0m"


def log(msg, color=COLOR_CYAN):
    """Print a message with optional color."""
    print(f"{color}{msg}{COLOR_RESET}")

OLD_PACKAGE = "com.example.modtemplate"
OLD_MOD_ID = "examplemod"
OLD_MOD_NAME = "Example Mod"
OLD_AUTHOR = "yourname"

base_package = input(f"Base package [{OLD_PACKAGE}]: ") or OLD_PACKAGE
mod_id = input(f"Mod id [{OLD_MOD_ID}]: ") or OLD_MOD_ID
mod_name = input(f"Mod name [{OLD_MOD_NAME}]: ") or OLD_MOD_NAME
author = input(f"Author [{OLD_AUTHOR}]: ") or OLD_AUTHOR


def to_camel(text: str) -> str:
    return "".join(part.capitalize() for part in re.split(r"[\s_-]+", text))

class_base = to_camel(mod_name)

replacements = {
    OLD_PACKAGE: base_package,
    OLD_MOD_ID: mod_id,
    OLD_MOD_NAME: mod_name,
    OLD_AUTHOR: author,
    "TemplateMod": f"{class_base}Mod",
    "TemplateFabric": f"{class_base}Fabric",
    "TemplateForge": f"{class_base}Forge",
    "TemplateNeoForge": f"{class_base}NeoForge",
}

log("Updating file contents...")
exclude_dirs = {'.git', 'build', '.gradle'}
script_path = Path(__file__).resolve()
for path in Path('.').rglob('*'):
    if path.is_file() and path.resolve() != script_path and not exclude_dirs.intersection(path.parts):
        try:
            text = path.read_text()
        except UnicodeDecodeError:
            continue
        replaced = False
        for old, new in replacements.items():
            if old in text:
                text = text.replace(old, new)
                replaced = True
        if replaced:
            path.write_text(text)
            log(f"  {path}", COLOR_GREEN)

old_parts = OLD_PACKAGE.split('.')
new_parts = base_package.split('.')
log("Renaming packages...")
for module in ['common', 'fabric', 'forge', 'neoforge']:
    src = Path(module, 'src', 'main', 'java')
    old_dir = src.joinpath(*old_parts)
    if old_dir.exists():
        new_dir = src.joinpath(*new_parts)
        new_dir.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(old_dir), str(new_dir))
        log(f"  {old_dir} -> {new_dir}", COLOR_GREEN)
        try:
            shutil.rmtree(src.joinpath(old_parts[0]))
        except OSError:
            pass

log("Renaming files...")
for old, new in {
    OLD_MOD_ID: mod_id,
    "TemplateMod": f"{class_base}Mod",
    "TemplateFabric": f"{class_base}Fabric",
    "TemplateForge": f"{class_base}Forge",
    "TemplateNeoForge": f"{class_base}NeoForge",
}.items():
    for path in Path('.').rglob(f'*{old}*'):
        if path.is_file():
            new_path = path.with_name(path.name.replace(old, new))
            path.rename(new_path)
            log(f"  {path} -> {new_path}", COLOR_GREEN)

log("Template initialized.", COLOR_GREEN)

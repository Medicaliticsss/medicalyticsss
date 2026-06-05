# git-filter-repo --commit-callback body (not a full Python module)
import re

CURSOR_EMAIL = b"cursoragent@cursor.com"
TARGET_NAME = b"gabost10297"
TARGET_EMAIL = b"gabost10297@users.noreply.github.com"
CO_AUTHOR_RE = re.compile(rb"^Co-authored-by:.*$", re.MULTILINE)
MADE_WITH_CURSOR_RE = re.compile(
    rb"^\s*(Made-with|Made with):.*[Cc]ursor.*$", re.MULTILINE
)

if commit.author_email == CURSOR_EMAIL or commit.committer_email == CURSOR_EMAIL:
    commit.author_name = TARGET_NAME
    commit.author_email = TARGET_EMAIL
    commit.committer_name = TARGET_NAME
    commit.committer_email = TARGET_EMAIL
    message = commit.message
    message = CO_AUTHOR_RE.sub(b"", message)
    message = MADE_WITH_CURSOR_RE.sub(b"", message)
    message = re.sub(rb"\n{3,}", b"\n\n", message).rstrip() + b"\n"
    commit.message = message

"""문서(.md)에서 제품명 'SecPortal' 을 'KSecPortal' 로 바꾼다.

코드 식별자·경로·컨테이너명·계정은 건드리지 않는다:
  - com.monosun.secportal / secportal-backend / secportal@monosun.com (소문자라 애초에 매칭 안 됨)
  - SecPortalApplication (뒤에 영문자가 이어지면 제외)
  - KSecPortal (앞에 영문자가 있으면 제외 → KKSecPortal 방지)

사용: python scripts/rename_secportal_docs.py [--apply]
"""
import io
import os
import re
import sys

PATTERN = re.compile(r'(?<![A-Za-z0-9_./@-])SecPortal(?![A-Za-z0-9_])')
SKIP_FILES = {'CLAUDE.md'}
SKIP_DIRS = {'.git', 'node_modules', 'dist', 'build', '.gradle', 'backups'}

# 앱이 실제로 보내는 알림 제목 접두어. 문서가 수신 예시를 그대로 옮긴 것이므로
# 여기를 바꾸면 문서가 실제 동작과 어긋난다 → 코드 문자열과 함께 바꿔야 할 대상이라 제외한다.
LITERAL_SUBJECT = '[SecPortal]'
SENTINEL = '\x00SUBJECT\x00'


def convert(text):
    """제품명만 KSecPortal 로 바꾸고, 알림 제목 접두어 리터럴은 보존한다."""
    protected = text.replace(LITERAL_SUBJECT, SENTINEL)
    replaced = PATTERN.sub('KSecPortal', protected)
    return replaced.replace(SENTINEL, LITERAL_SUBJECT)


def count(text):
    return len(PATTERN.findall(text.replace(LITERAL_SUBJECT, SENTINEL)))


def walk_markdown(root='.'):
    for base, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for name in files:
            if not name.endswith('.md'):
                continue
            path = os.path.join(base, name).replace(os.sep, '/')
            if path.startswith('./'):
                path = path[2:]
            if os.path.basename(path) in SKIP_FILES:
                continue
            yield path


def main():
    apply = '--apply' in sys.argv
    total_files = total_hits = 0

    for path in walk_markdown():
        try:
            text = io.open(path, encoding='utf-8').read()
        except (UnicodeDecodeError, OSError):
            continue
        hits = count(text)
        if not hits:
            continue
        total_files += 1
        total_hits += hits
        print('%4d  %s' % (hits, path))
        if apply:
            io.open(path, 'w', encoding='utf-8', newline='\n').write(convert(text))

    print('---')
    print('%s 파일 %d개 / %d건' % ('변경' if apply else '대상(미적용)', total_files, total_hits))


if __name__ == '__main__':
    main()

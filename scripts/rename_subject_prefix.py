"""알림 제목 접두어 [SecPortal] → [KSecPortal] (코드 + 문서의 수신 예시).

제품명 통일 작업의 마무리. 코드가 실제로 보내는 문자열과 문서의 수신 예시를 함께 바꾼다.
사용: python scripts/rename_subject_prefix.py [--apply]
"""
import io
import os
import sys

OLD = '[SecPortal]'
NEW = '[KSecPortal]'
TARGET_EXT = ('.java', '.md', '.vue', '.js')
SKIP_DIRS = {'.git', 'node_modules', 'dist', 'build', '.gradle', 'backups'}
SKIP_FILES = {'CLAUDE.md'}


def main():
    apply = '--apply' in sys.argv
    files = hits = 0

    for base, dirs, names in os.walk('.'):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for name in names:
            if not name.endswith(TARGET_EXT) or name in SKIP_FILES:
                continue
            path = os.path.join(base, name).replace(os.sep, '/').lstrip('./')
            try:
                text = io.open(path, encoding='utf-8').read()
            except (UnicodeDecodeError, OSError):
                continue
            n = text.count(OLD)
            if not n:
                continue
            files += 1
            hits += n
            print('%4d  %s' % (n, path))
            if apply:
                io.open(path, 'w', encoding='utf-8', newline='\n').write(text.replace(OLD, NEW))

    print('---')
    print('%s 파일 %d개 / %d건' % ('변경' if apply else '대상(미적용)', files, hits))


if __name__ == '__main__':
    main()

import sys

def append_to_file(filepath, content):
    with open(filepath, 'a', encoding='utf-8') as f:
        f.write(content)
    print(f'Appended {len(content)} chars to {filepath}')

if __name__ == '__main__':
    if len(sys.argv) >= 3:
        append_to_file(sys.argv[1], sys.argv[2])
    else:
        print('Usage: python append_content.py <filepath> <content>')

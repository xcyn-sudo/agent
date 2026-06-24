import sys
content = sys.argv[1]
with open('/d/Javacode/agent-qr/doc/p2-prompt.md', 'a', encoding='utf-8') as f:
    f.write(content)
print('Appended successfully, length:', len(content))

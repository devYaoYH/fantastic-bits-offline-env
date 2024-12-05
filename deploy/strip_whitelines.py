import sys

def copy_filtered_lines(source_file, destination_file):
    """
    Copies lines from a source file to a destination file, filtering out:
    - Lines starting with "//" or "/*" or "*" or "*/"
    - Lines containing only whitespace

    Args:
        source_file (str): Path to the source file.
        destination_file (str): Path to the destination file.
    """

    with open(source_file, 'r') as src, open(destination_file, 'w') as dst:
        for line in src:
            # Strip leading and trailing whitespace
            stripped_line = line.strip()

            # Check if the line is not empty and doesn't start with a comment
            if stripped_line and not stripped_line.startswith(('/', '*')):
                dst.write(stripped_line + "\n")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python script.py source_file destination_file")
        sys.exit(1)

    source_file = sys.argv[1]
    destination_file = sys.argv[2]

    copy_filtered_lines(source_file, destination_file)

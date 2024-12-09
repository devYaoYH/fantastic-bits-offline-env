import subprocess
import sys
from typing import List, Tuple
import re
import numpy as np

def run_brutaltester(rationality: float) -> Tuple[float, float, float]:
    command = [
        'java', '-jar', 'cg-brutaltester-1.0.0-SNAPSHOT.jar',
        '-r', 'java -jar referee.jar',
        '-p1', 'python3 naivePlayer.py',
        '-p2', f'java -jar mcts.jar -r={rationality}',
        '-t', '4',
        '-n', '32',
        '-s',
        '-l', './logs/',
        '-o',
        '-m'
    ]
    
    # Capture the output
    result = subprocess.run(command, text=True, capture_output=True)
    
    # Extract scores using regex
    output = result.stdout
    
    # Player 1's score is in the first row: "| Player 1 |          | 34.38%   |"
    player1_pattern = r"Player 1 \|.*\| ([\d.]+)%"
    # Player 2's score is in the second row: "| Player 2 | 62.50%   |          |"
    player2_pattern = r"Player 2 \| ([\d.]+)%"
    
    player1_match = re.search(player1_pattern, output)
    player2_match = re.search(player2_pattern, output)
    
    p1_score = float(player1_match.group(1)) if player1_match else 0.0
    p2_score = float(player2_match.group(1)) if player2_match else 0.0
    
    return (rationality, p2_score, p1_score)

if __name__ == '__main__':
    rationalities = np.arange(0.0, 1.1, 0.1)
    
    for rationality in rationalities:
        rat, p2_score, p1_score = run_brutaltester(rationality)
        print(f"{rat:.1f} {p2_score:.2f} {p1_score:.2f}")
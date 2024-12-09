java -jar cg-brutaltester-1.0.0-SNAPSHOT.jar -r "java -jar referee.jar" -p1 "python3 naivePlayer.py" -p2 "java -jar mcts.jar $@" -t 4 -n 256 -s -l "./logs/" -o -m

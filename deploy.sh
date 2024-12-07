mkdir deploy
cp -r java deploy/
cd deploy
rm java/com/codingame/game/Referee.java
rm -r java/com/codingame/algorithms
rm java/com/codingame/agent/alphaBetaPlayer.java
java -jar CGFileMerge-1.0.2.jar java Output.java
python3 strip_whitelines.py Output.java Deploy.java
cp Deploy.java ../
cd ..

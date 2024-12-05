# Clean up previous version if any
rm mcts.jar
# Prepare build directory
mkdir -p build
# Generate .class files
javac -d ./build java/com/codingame/agent/*.java java/com/codingame/game/Simulator.java java/com/codingame/game/GameNode.java java/com/codingame/algorithms/GameTree.java java/com/codingame/game/WizardAction.java java/com/codingame/game/Action.java || exit 1
# Generate .jar
cd build
echo "Main-Class: com.codingame.agent.AlphaBetaPlayer" > MANIFEST.MF
jar cfm mcts.jar MANIFEST.MF com/codingame/game/*.class com/codingame/agent/*.class com/codingame/algorithms/*.class
# Copy the generated .jar file up one directory level
cp mcts.jar ../
cd ../
# Test the newly build player
java -jar cg-brutaltester-1.0.0-SNAPSHOT.jar -r "java -jar referee.jar" -p1 "python3 naivePlayer.py" -p2 "java -jar mcts.jar" -t 1 -n 1 -l "./logs/" -o
# Output the last 50 lines of the log file for verification
tail -n 50 logs/game1.log


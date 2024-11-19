# Brutal Tester with Fantastic Bits compatibility

## Running Instructions

The pre-built .jar files can be used out-of-the-box to execute Fantastic Bits games with the included naivePlayer.py bot.

You can execute a game and emit logs into the `/logs/` folder with the following command (note the `-o` at the end, this is necessary to direct brutaltester to run with the OldGameThread code which I've modified):

```
java -jar cg-brutaltester-1.0.0-SNAPSHOT.jar -r "java -jar referee.jar" -p1 "python3 naivePlayer.py" -p2 "python3 naivePlayer.py" -t 1 -n 1 -l "./logs/" -o
```

## Building Instructions

Two .jar files need to be built, first the cg-brutaltester program should be re-built if one modifies the OldGameThread I've hijacked to comform to the special Fantastic Bits Referee command format.

### cg-brutaltester .jar building

Navigate into the cg-brutaltester directory and run `mvn package`. Maven is required and can be installed on macOS using `brew install maven`.

A target subdirectory should be created under the cg-brutaltester directory which contains the packaged .jar file. Which should be named `cg-brutaltester-1.0.0-SNAPSHOT.jar` by default. If one is using the above running instruction sample command verbatim, one should copy that newly packaged .jar out into the base directory of this repo.

### referee.jar building

Building the referee jar is slightly more tricky. The Referee.java code from: https://github.com/SpiritusSancti5/codinGame/blob/master/Referees/Fantastic%20Bits/Referee.java has been copied into the `java/com/codingame/game/` directory and packaged under `com.codingame.game`. The following command generates the necessary `.class` files into a `/build` subdirectory:

```
javac -d ./build Referee.java
```

After this, one needs to manually create a java jar manifest file specifying the main class of the Referee:

```
echo "Main-Class: com.codingame.game.Referee" > META-INF/MANIFEST.MF
```

This should now allow one to run the following from within the `/build` subdirectory to package the .jar file:

```
jar cmvf META-INF/MANIFEST.MF referee.jar *.class
```

This should result in a `referee.jar` file created in `/build` directory and should be copied out into the base git repo directory where one can then run the running instruction command verbatim (else one could just modify and direct the referee execution command appropriately to the referee.jar file).


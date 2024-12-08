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

## Example Java Agent using Referee

`java/com/codingame/agent/mctsPlayer.java` contains wrapper code for an example agent which initializes a Referee object and interacts with it via input/output byte streams to simulate game turns.

Test the player by running the `compileAndTestJavaPlayer.sh` bash script. This generates a `mcts.jar` executable which we have ignored in the .gitignore.

# Game Agents

## Heuristic Base player

Simply head straight towards the nearest snaffle and attempt to capture it, thereafter, throw it towards the opponent's goal posts.

## Forward search player

2 methods were attempted:

1.  Simultaneous action generation, lists of actions were generated for each wizard then the cross-product taken to give the action pair for the turn.
2.  Decoupled action generation, lists of separate action pairs were generated for each wizard holding the other wizard fixed in position for the turn. The best actions are mixed together to get the final action pair for the turn.

Method (1) is truely complete forward-search and will cover the possible interactions between wizards. Method (2) is much faster since useful interactions between wizards aren't that frequently occuring. |A| * |A| for method (1) and 2*|A| for method (2).

Comparing their performance against the benchmark heuristic player, the methods are comparable when searching to depth 1 (immediate look-forward).

Method (2) was able to reach depth 2 forward search within reasonable timeframe, but method (1) was not.

### Parameters to tune

Within the examplePlayer.java Player class:

```
    // Random number generater has static seed for testing.
    static Random rand = new Random(0);
    // Ablation coefficient, agent takes a random action with probability 1-RATIONALITY_COEFF.
    static Double RATIONALITY_COEFF = 1.0;
    // If > 0, a rollout is performed with the heuristic state score
    // taken as reward and discounted by GAMMA each turn.
    static int ROLLOUT_DEPTH = 0;
    // The expected value is returned, so an average over the following
    // number of rollouts.
    static int ROLLOUT_WIDTH = 10;
    // Future reward discounting factor. We're really uncertain about our
    // future hence the low gamma value.
    static Double GAMMA = 0.7;

    /* Heuristics:
     *   1. distance of wizards to snaffles
     *   2. distance of snaffles to goal
     *   3. distance of bludgers to wizards
     *   4. magic score
     *   5. score score
     */
    // Best hand-tuned parameters so far:
    // 2, 800, 0.1, 1, 1000 (~78.13% win-rate against heuristic in 32 games)
    private static List<Double> geneticParams = List.of(1.0, 5.0, 0.1, 1.0, 1000.0);

    // Searching strategy, simultaneous or decoupled
    private static Boolean simultaneousSearching = false;
```

The `geneticParams` were set up for tuning by a larger overall GA optimization algorithm, however, this is yet incomplete at the moment. Instead, one can use the following script to hand-tune these values by passing along a list of values in the command used to execute the player agent code.

Usage for `geneticTest.sh`:

```
./geneticTest.sh -params=2,500,0.1,1,1000
```

This will execute 32 games with the players swapped in position each other round for fairness.

Currently this script only tests against the benchmark, but can be trivially modified to also work for two search agent players against each other with different parameters.

Some other parameters available in our search agent player:

- Rationality parameter (e.g.): `-r=0.9`

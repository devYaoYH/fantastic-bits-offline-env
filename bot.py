import sys
import math

from collections import namedtuple

# Grab Snaffles and try to throw them through the opponent's goal!
# Move towards a Snaffle and use your team id to determine where you need to throw it.

my_team_id = int(input())  # if 0 you need to score on the right of the map, if 1 you need to score on the left

# Initialize namedtuples
Wizard = namedtuple('Wizard', ['q', 'v', 'state'])
Snaffle = namedtuple('Snaffle', ['q', 'v'])

def distance(obj1, obj2):
    x1, y1 = obj1[0]
    x2, y2 = obj2[0]
    return math.hypot(x1 - x2, y1 - y2)

# Returns the index of the object in objects with the lowest penalty relative to agent
def best(agent, objects, penalty):
    best = objects[0]
    lowest_penalty = penalty(agent, best)
    for object in objects:
        if penalty(agent, object) < lowest_penalty:
            best = object
            lowest_penalty = penalty(agent, object)    


# game loop
while True:
    my_score, my_magic = [int(i) for i in input().split()]
    opponent_score, opponent_magic = [int(i) for i in input().split()]
    entities = int(input())  # number of entities still in game

    snaffles = []
    wizards = []
    opponents = []

    for i in range(entities):
        inputs = input().split()
        entity_id = int(inputs[0])  # entity identifier
        entity_type = inputs[1]  # "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
        x = int(inputs[2])  # position
        y = int(inputs[3])  # position
        vx = int(inputs[4])  # velocity
        vy = int(inputs[5])  # velocity
        state = int(inputs[6])  # 1 if the wizard is holding a Snaffle, 0 otherwise

        if entity_type == 'WIZARD' or entity_type == 'OPPONENT_WIZARD':
            wizards.append(Wizard(q=(x, y), v=(vx, vy), state=state))

        if entity_type == 'SNAFFLE':
            snaffles.append(Snaffle(q=(x, y), v=(vx, vy)))

    for i in range(2):

        # Write an action using print
        # To debug: print("Debug messages...", file=sys.stderr, flush=True)
    

        # Edit this line to indicate the action for each wizard (0 ≤ thrust ≤ 150, 0 ≤ power ≤ 500)
        # i.e.: "MOVE x y thrust" or "THROW x y power"
        
        if wizards[i].state:
            # If you are holding the Snaffle, throw it with maximum power at the center of the goal
            if my_team_id:
                print("THROW 16000 3750 500")
            else:
                print("THROW 0 3750 500")
        else:
            # Move with maximum thrust towards the nearest snaffle
            target = best(wizards[i], snaffles, distance)
            x, y = target.q
            print(f"MOVE {x} {y} 150")



import sys
import math
import time

INIT_TIME = time.time()
DEBUG_SIMUL_TIME = False


def mark(msg):
    # Print debugging message with timestamp."""
    print(msg+"{0}".format((time.time()-INIT_TIME)*1000), file=sys.stderr)
    return None


mark("Starting execution: ")

#####################
# Primitive Classes #
#####################


class Position():
    """Describe a singular 2d Point."""

    def __init__(self, x, y):
        self.x = x
        self.y = y

    def add(self, vec):
        newPos = Position(self.x + vec.vx, self.y + vec.vy)
        return newPos

    def sub(self, vec):
        newPos = Position(self.x - vec.vx, self.y - vec.vy)
        return newPos

    def distTo(self, obj):
        if (isinstance(obj, Position)):
            dx = self.x - obj.x
            dy = self.y - obj.y
            tmpDist = math.sqrt(dx**2 + dy**2)
            return tmpDist
        return None

    def dirTo(self, obj):
        if (isinstance(obj, Position)):
            dx = obj.x - self.x
            dy = obj.y - self.y
            tmpAng = math.atan2(dy, dx)
            return tmpAng
        return None

    def vecTo(self, obj):
        if (isinstance(obj, Position)):
            newDirTo = self.dirTo(obj)
            newDistTo = self.distTo(obj)
            newDx = newDistTo*math.cos(newDirTo)
            newDy = newDistTo*math.sin(newDirTo)
            newVec = Vector(newDx, newDy)
            return newVec
        return None


class Vector():

    def __init__(self, vx, vy):
        self.vx = vx
        self.vy = vy
        self.angle = math.atan2(self.vy, self.vx)
        self.length = math.sqrt(self.vx**2 + self.vy**2)

    def add(self, vec):
        newVec = Vector(self.vx + vec.vx, self.vy + vec.vy)
        return newVec

    def sub(self, vec):
        newVec = Vector(self.vx - vec.vx, self.vy - vec.vy)
        return newVec

    def mult(self, const):
        newVec = Vector(self.vx*const, self.vy*const)
        return newVec

    def dot(self, vec):
        dotPdt = self.vx*vec.vx + self.vy*vec.vy
        return dotPdt

    def normalize(self):
        if self.length > 0:
            return Vector(self.vx/self.length, self.vy/self.length)
        else:
            return Vector(0, 0)


class Line():

    def __init__(self, origin: Position, target: Position):
        self.origin = origin
        self.target = target


# Game Constants
MY_TEAM = int(input())  # 0 ==> Score Right | 1 ==> Score Left

MAX_THRUST = 150
MAX_POWER = 500
MIN_THRUST = 50
MIN_POWER = 250
MIN_IMPULSE = 100

FIELD_LENGTH = 16000
FIELD_WIDTH = 7500

# Spell Info
OBLIVIATE_COST = 5
OBLIVIATE_DURATION = 4
PETRIFICUS_COST = 10
PETRIFICUS_DURATION = 1
ACCIO_COST = 15
ACCIO_DURATION = 6
FLIPENDO_COST = 20
FLIPENDO_DURATION = 3

# Wizard Info
WIZARD_RADIUS = 400
WIZARD_FRICTION = 0.75
WIZARD_MASS = 1.0
WIZARD_COOLDOWN_CATCH = 3

# Snaffle Info
SNAFFLE_RADIUS = 150
SNAFFLE_FRICTION = 0.75
SNAFFLE_MASS = 0.5

# Bludger Info
BLUDGER_RADIUS = 200
BLUDGER_FRICTION = 0.90
BLUDGER_MASS = 8.0

# Map Constants
GOAL_RADIUS = 300

WALL_TOP = Line(Position(0, 0), Position(FIELD_LENGTH, 0))
WALL_BOTTOM = Line(Position(0, FIELD_WIDTH), Position(FIELD_LENGTH, 0))
WALL_LEFT = Line(Position(0, 0), Position(0, FIELD_WIDTH))
WALL_RIGHT = Line(Position(FIELD_LENGTH, 0), Position(0, FIELD_WIDTH))

GOAL_LEFT = Line(Position(0, 1750+GOAL_RADIUS+SNAFFLE_RADIUS), Position(0, 5750-GOAL_RADIUS-SNAFFLE_RADIUS))
GOAL_RIGHT = Line(Position(FIELD_LENGTH, 1750+GOAL_RADIUS+SNAFFLE_RADIUS), Position(FIELD_LENGTH, 5750-GOAL_RADIUS-SNAFFLE_RADIUS))

GOAL = (GOAL_RIGHT, GOAL_LEFT)

# Game State
PLAYER_OBJECTS = dict()
MY_SCORE = 0
EN_SCORE = 0
MY_MAGIC = 0
EN_MAGIC = 0

################
# Game Objects #
################

class GameObject():
    def __init__(self, entity_id, entity_type, position, velocity):
        self.entity_id = entity_id
        self.entity_type = entity_type
        self.position = Position(position.x, position.y)
        self.velocity = Vector(velocity.vx, velocity.vy)


class Wizard(GameObject):
    def __init__(self, entity_id, entity_type, position, velocity, state, team_id):
        super().__init__(entity_id, entity_type, position, velocity)
        self.has_snaffle = state == 1
        self.is_my_team = team_id == MY_TEAM
        self.team_id = team_id


class Snaffle(GameObject):
    def __init__(self, entity_id, entity_type, position, velocity, state):
        super().__init__(entity_id, entity_type, position, velocity)
        self.is_grabbed = state == 1


class Bludger(GameObject):
    def __init__(self, entity_id, entity_type, position, velocity, state):
        super().__init__(entity_id, entity_type, position, velocity)
        self.victim_id = state
        self.has_victim = state >= 0


class GameState():
    def __init__(self, wizards, snaffles, bludgers):
        self.wizards = {ID: wizards[ID] for ID in wizards}
        self.snaffles = {ID: snaffles[ID] for ID in snaffles}
        self.bludgers = {ID: bludgers[ID] for ID in bludgers}

    def copy(self):
        return GameState(self.wizards, self.snaffles, self.bludgers)


####################
# Heuristic Player #
####################

class Action():
    pass    


class Move(Action):
    def __init__(self, position, thrust=MAX_THRUST):
        self.x = position.x
        self.y = position.y
        self.thrust = thrust

    def __str__(self):
        return f"MOVE {round(self.x)} {round(self.y)} {round(self.thrust)}"


class Throw(Action):
    def __init__(self, position, power=MAX_POWER):
        self.x = position.x
        self.y = position.y
        self.power = power

    def __str__(self):
        return f"THROW {round(self.x)} {round(self.y)} {round(self.power)}"


class HeuristicPolicy():
    def __init__(self, state):
        self.state = state.copy()

    def get_action(self, entity_id=0):
        if entity_id not in self.state.wizards:
            raise ValueError(f"EntityID {entity_id} is not a Wizard.")
        wizard = self.state.wizards[entity_id]

        # Generate Snaffle Throw
        if wizard.has_snaffle:
            goal_top = GOAL[wizard.team_id].origin
            goal_bottom = GOAL[wizard.team_id].target

            if wizard.position.distTo(goal_top) > wizard.position.distTo(goal_bottom): # Shoot at TOP
                return Throw(goal_top)
            else:
                return Throw(goal_bottom)

        # Generate Target vector
        # Get nearest snaffle
        nearby_snaffles = sorted([(wizard.position.distTo(snaffle.position), snaffle) for snaffle in self.state.snaffles.values()])
        nearest_snaffle = nearby_snaffles[0][1]
        target_vector = wizard.position.vecTo(nearest_snaffle.position)

        # Generate Deflection vectors

        # for bludger in self.state.bludgers.values():
        #     deflection = bludger.position.vecTo(wizard.position)
        #     min_dist = BLUDGER_RADIUS + WIZARD_RADIUS + bludger.velocity.length
        #     deflection_weight = min_dist/(deflection.length-min_dist)
        #     target_vector = target_vector.add(deflection.normalize().mult(deflection_weight * min_dist))

        # Constitute Thrust vector
        return Move(wizard.position.add(target_vector), thrust=min(MAX_THRUST, target_vector.length))


mark("Finished declarations: ")
#############
# Game Loop #
#############
while True:
    MY_SCORE, MY_MAGIC = [int(i) for i in input().split()]
    EN_SCORE, EN_MAGIC = [int(i) for i in input().split()]

    # State Representation
    snaffles = dict()
    bludgers = dict()
    wizards = dict()

    num_entities = int(input())  # Number of entities in game round
    for i in range(num_entities):
        entity_id, entity_type, x, y, vx, vy, state = input().split()
        entity_id = int(entity_id)
        x = int(x)
        y = int(y)
        vx = int(vx)
        vy = int(vy)
        state = int(state)

        if (entity_type == "WIZARD"):
            wizards[entity_id] = Wizard(entity_id, entity_type, Position(x, y), Vector(vx, vy), state, MY_TEAM)
        elif (entity_type == "OPPONENT_WIZARD"):
            wizards[entity_id] = Wizard(entity_id, entity_type, Position(x, y), Vector(vx, vy), state, int(not MY_TEAM))
        elif (entity_type == "SNAFFLE"):
            snaffles[entity_id] = Snaffle(entity_id, entity_type, Position(x, y), Vector(vx, vy), state)
        elif (entity_type == "BLUDGER"):
            bludgers[entity_id] = Bludger(entity_id, entity_type, Position(x, y), Vector(vx, vy), state)

    game_state = GameState(wizards, snaffles, bludgers)
    heuristic_policy = HeuristicPolicy(game_state)

    mark("Loaded round data: ")

    # Execute turns for our wizards :D
    for ID in sorted(list(wizards.keys())):
        wizard = wizards[ID]
        if wizard.is_my_team:
            print("===\nExecuting for Wizard {0}\n===".format(ID), file=sys.stderr)
            turnCmd = heuristic_policy.get_action(entity_id=ID)
            print(turnCmd)

    mark("Finished turn execution: ")

    INIT_TIME = time.time()

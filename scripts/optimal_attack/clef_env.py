# Environment for CLEF
import numpy as np


class ClefEnv:
    NUM_TIME_SLOTS = 12
    NUM_LEVELS = 4  # number of levels in one RLFD cycle
    MIN_T = 1   # T is the period of a level in RLFD
    MAX_T = 1
    T_LIST = range(MIN_T, MAX_T + 1, 1)
    # MAX_T = 10 * NUM_LEVELS
    # NUM_COUNTER = 100
    # THRESHOLD = 1
    # EARDET_LIMIT = 100
    # OUTBOUND_CAPACITY = 10000

    # Actions:
    # 1 -> send traffic to the limit of EARDet, in a time slot
    # 0 -> send no traffic in a time slot
    ACTION_SPACE = [0, 1]

    def __init__(self):
        self.reset()

    def reset(self):
        self.status = [0] * self.NUM_TIME_SLOTS
        self.next_idx = 0           # indicate the slot in timeline
        self.next_cycle_idx = 0     # indicate the slot in current cycle
        self.violate_amount = 0     # number of "true" slot so far
        self.cur_prob = list()
        self.cur_T = 1

        n = len(self.T_LIST)
        self.T_nums = dict()
        slots_left = self.NUM_TIME_SLOTS
        for i in range(0, n):
            T = self.T_LIST[i]
            self.T_nums[T] = self.NUM_TIME_SLOTS / n / T / self.NUM_LEVELS
            slots_left -= self.T_nums[T] * T * self.NUM_LEVELS

        # fill up the slots left by the MIN_T
        T_min = self.T_LIST[0]
        self.T_nums[T_min] += \
            slots_left / T_min / self.NUM_LEVELS
        # print self.T_nums
        return self.status

    def get_action_space(self):
        return self.ACTION_SPACE

    def _pick_T_randomly(self):
        if len(self.T_nums.keys()) == 0:
            return None

        key_idx = np.random.randint(len(self.T_nums.keys()))
        T = self.T_nums.keys()[key_idx]
        self.T_nums[T] -= 1
        if self.T_nums[T] == 0:
            del self.T_nums[T]
        # print "T = " + str(T) + ", T_nums = " + str(self.T_nums)
        return T

    def step(self, action_idx):
        # s1, r, d, _ = env.step(a)
        self.last_action = self.ACTION_SPACE[action_idx]
        if self.next_cycle_idx == 0:
            # randomly select a new cycle
            self.cur_prob = [0.] * self.NUM_LEVELS
            self.cur_T = self._pick_T_randomly()
            if self.cur_T is None:
                return self.status, 0, True

        r = 0
        d = False
        action = self.ACTION_SPACE[action_idx]
        level = self.next_cycle_idx / self.cur_T
        self.status[self.next_idx] = action
        if action == 1:
            # we have approximation over prob here
            self.cur_prob[level] = 1.
            self.violate_amount += 1
            r = 1
        else:
            # we have approximation over prob here
            self.cur_prob[level] = 0.

        self.next_cycle_idx += 1
        self.next_idx += 1

        if self.next_cycle_idx == self.NUM_LEVELS * self.cur_T:
            # if this is the last slot of this cycle
            # see whether we can detect the large flow
            detect_prob = 1.
            for l in range(0, self.NUM_LEVELS):
                detect_prob *= self.cur_prob[l]

            if np.random.rand(1) <= detect_prob or\
                    len(self.T_nums.keys()) == 0:
                d = True

            # rewind the cycle idx to the beginning
            self.next_cycle_idx = 0

        return self.status, r, d

    def render(self):
        print "action = %d" % self.last_action

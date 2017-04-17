# Environment for CLEF
import numpy as np


class ClefEnv:
    NUM_TIME_SLOTS = 1000
    NUM_LEVELS = 4  # number of levels in one RLFD cycle
    # T is the period of a level in RLFD
    T_LIST = [1, 2, 3, 7, 10]
    P_LIST = [4. / 11, 3. / 11, 2. / 11, 1. / 11, 1. / 11]
    NEG_REWARD = -1 * max(T_LIST) * NUM_LEVELS
    NEG_REWARD_PROB = 1.0
    # MAX_T = 10 * NUM_LEVELS
    # NUM_COUNTER = 100
    # THRESHOLD = 1
    # EARDET_LIMIT = 100
    # OUTBOUND_CAPACITY = 10000

    # Actions:
    # 1 -> send traffic to the limit of EARDet, in a time slot
    # 0 -> send no traffic in a time slot
    ACTION_SPACE = [0, 1]

    def __init__(self, config=None):
        if config is not None:
            if "NUM_TIME_SLOTS" in config:
                self.NUM_TIME_SLOTS = config["NUM_TIME_SLOTS"]
            if "NUM_LEVELS" in config:
                self.NUM_LEVELS = config["NUM_LEVELS"]
            if "T_LIST" in config:
                self.T_LIST = config["T_LIST"]
            if "P_LIST" in config:
                self.P_LIST = config["P_LIST"]
            if "NEG_REWARD" in config:
                self.NEG_REWARD = config["NEG_REWARD"]
            if "NEG_REWARD_PROB" in config:
                self.NEG_REWARD_PROB = config["NEG_REWARD_PROB"]

            if len(self.P_LIST) != len(self.T_LIST):
                print "size of P_LIST != size of T_LIST!"
                exit()

        self.P_ACCU_LIST = list()
        p_accu = 0.
        for i in range(len(self.P_LIST)):
            p_accu += self.P_LIST[i]
            self.P_ACCU_LIST.append(p_accu)

        self.reset()

    def reset(self):
        self.status = [0.5] * self.NUM_TIME_SLOTS
        self.next_idx = 0           # indicate the slot in timeline
        self.next_cycle_idx = 0     # indicate the slot in current cycle
        self.violate_amount = 0     # number of "true" slot so far
        self.cur_prob = list()
        self.cur_T = 1
        return self.status

    def get_action_space(self):
        return self.ACTION_SPACE

    def _pick_T_randomly(self):
        rand = np.random.rand()
        key_idx = len(self.P_ACCU_LIST) - 1
        for i in range(len(self.P_ACCU_LIST)):
            if rand < self.P_ACCU_LIST[i]:
                key_idx = i
                break
        return self.T_LIST[key_idx]

    def step(self, action_idx):
        # s1, r, d, _ = env.step(a)
        self.last_action = self.ACTION_SPACE[action_idx]
        if self.next_cycle_idx == 0:
            # randomly select a new cycle
            self.cur_prob = [0.] * self.NUM_LEVELS
            self.cur_T = self._pick_T_randomly()

        r = 0
        d = False
        info = [False, False]   # [is end of level , is end of cycle]

        action = self.ACTION_SPACE[action_idx]
        level = self.next_cycle_idx / self.cur_T
        self.status[self.next_idx] = action
        if action == 1:
            # we have approximation over prob here
            self.cur_prob[level] = 1.
            self.violate_amount += 1
            r = 1

        self.next_cycle_idx += 1
        self.next_idx += 1

        if self.next_cycle_idx % self.cur_T == 0:
            info[0] = True

        if self.next_cycle_idx == self.NUM_LEVELS * self.cur_T:
            info[1] = True
            # if this is the last slot of this cycle
            # see whether we can detect the large flow
            prob_before_last_level = 1.
            for l in range(0, self.NUM_LEVELS - 1):
                prob_before_last_level *= self.cur_prob[l]
            detect_prob = prob_before_last_level * \
                self.cur_prob[self.NUM_LEVELS - 1]

            if np.random.rand(1) <= detect_prob:
                d = True
                if np.random.rand(1) <= self.NEG_REWARD_PROB:
                    r = self.NEG_REWARD

            # if prob_before_last_level == 1 and action == 0:
            #     r = 1   # the step that avoid detection

            # rewind the cycle idx to the beginning
            self.next_cycle_idx = 0

        if self.next_idx == self.NUM_TIME_SLOTS:
            # if it is the end, we stop it anyway
            d = True

        return self.status, r, d, info

    def render(self):
        print "action = %d" % self.last_action


if __name__ == '__main__':
    # for testing ClefEnv only
    import sys
    import json
    config = None   # using default setting
    if len(sys.argv) == 3 and sys.argv[1] == "--config":
        with open(sys.argv[2]) as config_file:
            config = json.load(config_file)
    env = ClefEnv(config=config)

    d = dict()
    num_T = len(env.T_LIST)
    for i in range(num_T):
        d[env.T_LIST[i]] = 0

    for _ in range(10000):
        T = env._pick_T_randomly()
        d[T] += 1
    print "T frequency = " + str(d)

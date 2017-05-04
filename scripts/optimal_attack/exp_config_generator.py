import sys

rl_config_dir = './mini_scale_2'
rl_config_prefix = 'config_ms_prob_'
if len(sys.argv) == 5 and sys.argv[1] == '--dir' and sys.argv[3] == '--prefix':
    rl_config_dir = sys.argv[2]
    rl_config_prefix = sys.argv[4]

PROB_LIST = range(0, 101, 2)

CONFIG_TEMPLATE = '''{{
    "EXP_NAME": "ms_prob_{prob}",
    "T_LIST": [1, 2, 3, 4],
    "P_LIST": [ 0.48,  0.24,  0.16,  0.12],
    "THRESHOLD_RATE": 0.3333333333333333,
    "MAX_PERIOD": 15,
    "NUM_LEVELS": 4,
    "NUM_TIME_SLOTS": 1000,
    "NEG_REWARD": -16,
    "INPUT_SIZE": 16,
    "NUM_EPISODES": 10000,
    "NUM_TEST_EPISODES": 1000,
    "BATCH_SIZE": 10,
    "LEARNING_RATE": 0.001,
    "DISCOUNT_FACTOR": 0.99,
    "NEG_REWARD_PROB": {prob_float},
    "TRAIN_CLEF": false,
    "POS_CLEF_REWARD": 100
}}
'''

for p in PROB_LIST:
    file_path = rl_config_dir + "/" + rl_config_prefix + str(p) + ".json"
    with open(file_path, 'w') as f:
        f.write(CONFIG_TEMPLATE.format(
            prob=p,
            prob_float=float(p) / 100))

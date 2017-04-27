import sys
import json
from utils import ValidChecker
from utils import render_traffic
from utils import TrafficIterator

T_LIST = range(1, 11)
NUM_LEVELS = 4
MAX_PERIOD = 24     # maximum period size to brute foce to


if __name__ == '__main__':
    config = None   # using default setting
    if len(sys.argv) == 3 and sys.argv[1] == "--config":
        with open(sys.argv[2]) as config_file:
            config = json.load(config_file)

    if config is not None:
        if "T_LIST" in config:
            T_LIST = config["T_LIST"]
        if "NUM_LEVELS" in config:
            NUM_LEVELS = config["NUM_LEVELS"]

    p = MAX_PERIOD
    T_max = max(T_LIST)
    checker = ValidChecker(T_LIST, NUM_LEVELS)
    max_rate = 0.
    max_pattern = []
    # traverse the traffic pattern from MAX_PERIOD to 2
    # skip period p if there is a p' > p and p' % p == 0,
    # to avoid considering duplicated pattern
    for p in range(MAX_PERIOD, 2, -1):
        # deduplication
        is_duplicate = False
        for checked_p in range(MAX_PERIOD, p, -1):
            if checked_p % p == 0:
                is_duplicate = True
                break
        if is_duplicate:
            continue

        it = TrafficIterator(p, checker=checker)
        pattern = it.next()
        while pattern is not None:
            num_periods = (NUM_LEVELS * T_max / p + 1) + 1
            traffic = pattern * num_periods
            # print "traffic: " + str(traffic)
            if not checker.is_detectable(traffic, period=p):
                rate = float(sum(pattern)) / p
                if rate > max_rate:
                    max_rate = rate
                    max_pattern = pattern
            pattern = it.next()

    print "max rate = %f" % max_rate
    render_traffic(max_pattern)

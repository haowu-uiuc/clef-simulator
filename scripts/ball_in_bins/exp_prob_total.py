import random
import sys

from collections import OrderedDict
from collections import defaultdict


def balls_in_bins_run(num_balls, num_bins, alpha):
    bins = [0] * num_bins

    # randomly throw the balls
    for i in range(0, num_balls):
        bin_index = random.randint(0, num_bins - 1)
        bins[bin_index] += 1

    # randomly pick a bin
    bin_index = random.randint(0, num_bins - 1)
    picked_value = bins[bin_index]

    # find the max value
    max_value = 0
    for i in range(0, num_bins):
        max_value = max(max_value, bins[i])

    if max_value < picked_value + alpha:
        return True, picked_value

    return False, picked_value


def exp_for_prob(num_repeats, num_balls, num_bins, num_levels, alpha):
    prob = 0

    for r in range(0, num_repeats):
        print 'alpah = %d, repeats = %d' % (alpha, r)
        n = num_balls
        m = num_bins
        passed = False
        for k in range(0, num_levels):
            passed, n = balls_in_bins_run(n, m, alpha)
            if not passed:
                break
        if passed:
            prob += 1

    prob = float(prob) / float(num_repeats)
    return prob


if __name__ == "__main__":
    repeats = 1000
    num_balls = 100000
    num_bins = 100
    num_levels = 2
    max_alpha = 80

    if len(sys.argv) == 5:
        repeats = int(sys.argv[1])
        num_balls = int(sys.argv[2])
        num_bins = int(sys.argv[3])
        max_alpha = int(sys.argv[4])

    print 'repates: %d, num_balls: %d, num_bins: %d' \
        % (repeats, num_balls, num_bins)

    out_file_name = "data/total-prob-%d-%d-%d.txt" % (
        num_bins, num_balls, num_levels)
    
    with open(out_file_name, 'w') as f:
        for a in range(1, max_alpha):
            prob = exp_for_prob(
                repeats, num_balls, num_bins, num_levels, a)
            f.write(str(a) + '\t' + str(prob) + '\n')

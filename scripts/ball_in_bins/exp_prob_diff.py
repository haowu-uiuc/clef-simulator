import random
import sys

from collections import OrderedDict
from collections import defaultdict


def balls_in_bins_run(num_balls, num_bins):
    bins = [0] * num_bins
    freqs = defaultdict(int)     # bin values -> frequency

    # randomly throw the balls
    for i in range(0, num_balls):
        bin_index = random.randint(0, num_bins - 1)
        bins[bin_index] += 1

    # randomly pick a bin
    bin_index = random.randint(0, num_bins - 1)
    picked_value = bins[bin_index]

    # sort the values in bins
    sorted_values = sorted(bins, reverse=True)
    max_value = sorted_values[0]

    # freqs
    for i in range(0, num_bins):
        value = bins[i]
        freqs[value] += 1

    diffs = list()
    for v in sorted_values:
        diff = v - picked_value
        diffs.append(diff)

    return freqs, max_value, diffs


def exp_for_prob(num_repeats, num_balls, num_bins):
    prob_pick_x = defaultdict(int)
    prob_max_x = defaultdict(int)
    prob_diffs = list()
    for i in range(0, num_bins):
        prob_diff = defaultdict(int)
        prob_diffs.append(prob_diff)

    for i in range(0, repeats):
        print 'round #' + str(i)
        round_result, max_value, diffs = balls_in_bins_run(num_balls, num_bins)

        prob_max_x[max_value] += 1. / num_repeats

        for k, v in round_result.iteritems():
            prob_pick_x[k] += v / float(num_repeats) / float(num_bins)

        for j in range(0, num_bins):
            prob_diffs[j][diffs[j]] += 1. / float(num_repeats)

    return prob_pick_x, prob_max_x, prob_diffs


if __name__ == "__main__":
    repeats = 1000
    num_balls = 10000
    num_bins = 100

    if len(sys.argv) == 4:
        repeats = int(sys.argv[1])
        num_balls = int(sys.argv[2])
        num_bins = int(sys.argv[3])

    print 'repates: %d, num_balls: %d, num_bins: %d' \
        % (repeats, num_balls, num_bins)

    out_file_name = "prob-pick-%d-%d.txt" % (num_bins, num_balls)
    max_value_out_file_name = "max-prob-%d-%d.txt" % (num_bins, num_balls)
    prob_diffs_out_file_name = "prob-diffs-%d-%d.txt" % (num_bins, num_balls)

    prob_pick_x, prob_max_x, prob_diffs = exp_for_prob(
        repeats, num_balls, num_bins)

    ordered_prob_max_x = OrderedDict(sorted(prob_max_x.items()))
    with open(max_value_out_file_name, 'w') as f:
        for k, v in ordered_prob_max_x.iteritems():
            f.write(str(k) + '\t' + str(v) + '\n')

    ordered_prob_pick_x = OrderedDict(sorted(prob_pick_x.items()))
    with open(out_file_name, 'w')as f:
        for k, v in ordered_prob_pick_x.iteritems():
            line = str(k) + '\t' + str(v) + '\n'
            f.write(line)

    with open(prob_diffs_out_file_name, 'w') as f:
        for i in range(0, num_bins):
            ordered_prob_diff = OrderedDict(sorted(prob_diffs[i].items()))
            for k, v in ordered_prob_diff.iteritems():
                line = '%d\t%d\t%f\n' % (i, k, v)
                f.write(line)

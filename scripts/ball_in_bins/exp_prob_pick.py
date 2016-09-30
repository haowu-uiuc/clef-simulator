import random
from collections import OrderedDict
from collections import defaultdict


def balls_in_bins_run(num_balls, num_bins):
    bins = [0] * num_bins
    result = defaultdict(int)     # bin values -> frequency
    max_value = 0

    # randomly throw the balls
    for i in range(0, num_balls):
        bin_index = random.randint(0, num_bins - 1)
        bins[bin_index] += 1
        max_value = max(max_value, bins[bin_index])

    # output the result
    for i in range(0, num_bins):
        value = bins[i]
        result[value] += 1

    return result, max_value


def exp_for_prob(num_repeats, num_balls, num_bins):
    prob_pick_x = defaultdict(int)
    prob_max_x = defaultdict(int)
    for i in range(0, repeats):
        print 'round #' + str(i) + '\t' + str(num_bins) + '-' + str(num_balls)
        round_result, max_value = balls_in_bins_run(num_balls, num_bins)
        prob_max_x[max_value] += 1. / num_repeats
        for k, v in round_result.iteritems():
            prob_pick_x[k] += v / float(num_repeats) / float(num_bins)
    return prob_pick_x, prob_max_x


if __name__ == "__main__":
    repeats = 1000
    num_balls = 10000
    num_bins = 100
    out_file_name = "epp_prob-pick-%d-%d.txt" % (num_bins, num_balls)
    max_value_out_file_name = "epp_max-prob-%d-%d.txt" % (num_bins, num_balls)

    prob_pick_x, prob_max_x = exp_for_prob(repeats, num_balls, num_bins)

    ordered_prob_pick_x = OrderedDict(sorted(prob_pick_x.items()))
    ordered_prob_max_x = OrderedDict(sorted(prob_max_x.items()))
    with open(max_value_out_file_name, 'w') as f:
        for k, v in ordered_prob_max_x.iteritems():
            f.write(str(k) + '\t' + str(v) + '\n')

    f = open(out_file_name, 'w')
    for k, v in ordered_prob_pick_x.iteritems():
        line = str(num_bins) + '\t' + str(num_balls) \
            + '\t' + str(k) + '\t' + str(v) + '\n'
        f.write(line)

    # for each max_value, calculate the case:
    # c_num_balls = num_balls - max_value
    # c_num_bins = num_bins - 1
    for max_value in ordered_prob_max_x.keys():
        c_num_bins = num_bins - 1
        c_num_balls = num_balls - max_value
        c_prob_pick_x, c_prob_max_x = exp_for_prob(
            repeats, c_num_balls, c_num_bins)

        ordered_c_prob_pick_x = OrderedDict(sorted(c_prob_pick_x.items()))
        for k, v in ordered_c_prob_pick_x.iteritems():
            line = str(c_num_bins) + '\t' + str(c_num_balls) \
                + '\t' + str(k) + '\t' + str(v) + '\n'
            f.write(line)

    f.close()

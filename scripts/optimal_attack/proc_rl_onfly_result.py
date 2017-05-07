import sys
import os.path
import operator

rl_result_dir = './mini_scale_2'
rl_result_prefix = 'rl_onfly_result_ms'
if len(sys.argv) == 5 and sys.argv[1] == '--dir' and sys.argv[3] == '--prefix':
    rl_result_dir = sys.argv[2]
    rl_result_prefix = sys.argv[4]

round_dict = dict()

for i in range(1, 101):
    file_path = rl_result_dir + '/' + rl_result_prefix\
        + '_prob_' + str(i) + '.txt'
    if os.path.isfile(file_path):
        with open(file_path, 'r') as f:
            is_first_line = True
            for line in f:
                if is_first_line:
                    is_first_line = False
                    continue
                strs = line.split()
                rnd = int(strs[0])
                v = float(strs[1])
                t = float(strs[2])
                r = float(strs[3])
                if rnd not in round_dict:
                    round_dict[rnd] = dict()

                data = round_dict[rnd]
                if r not in data:
                    data[r] = (v, t)
                elif data[r][1] < t:
                    data[r] = (v, t)

for rnd, data in round_dict.items():
    sorted_data = sorted(data.items(), key=operator.itemgetter(0))
    round_dict[rnd] = sorted_data

sorted_dict = sorted(round_dict.items(), key=operator.itemgetter(0))

output_path = rl_result_dir + '/' + rl_result_prefix + '.txt'
with open(output_path, 'w') as f:
    for rnd, rnd_dict in sorted_dict:
        for k, v in rnd_dict:
            f.write("%d\t%f\t%f\t%f\n" % (rnd, k, v[0], v[1]))

import sys
import os.path
import operator

rl_result_dir = './toy_scale'
rl_result_prefix = 'rl_result_ts'
if len(sys.argv) == 5 and sys.argv[1] == '--dir' and sys.argv[3] == '--prefix':
    rl_result_dir = sys.argv[2]
    rl_result_prefix = sys.argv[4]

data = dict()

for i in range(1, 101):
    file_path = rl_result_dir + '/' + rl_result_prefix\
        + '_prob_' + str(i) + '.txt'
    if os.path.isfile(file_path):
        with open(file_path, 'r') as f:
            f.readline()
            strs = f.readline().split()
            v = float(strs[0])
            t = float(strs[1])
            r = float(strs[2])
            if r not in data:
                data[r] = (v, t)
            elif data[r][1] < t:
                data[r] = (v, t)

sorted_data = sorted(data.items(), key=operator.itemgetter(0))

output_path = rl_result_dir + '/' + rl_result_prefix + '.txt'
with open(output_path, 'w') as f:
    for k, v in sorted_data:
        f.write("%f\t%f\t%f\n" % (k, v[0], v[1]))

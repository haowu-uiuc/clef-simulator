import os
from subprocess import call

rate_ids = dict()
num_counter = 7
spec_rate = 50000
rate_id = 0
rates = list()
rates.append(1)
rates.append(2)
rates.append(4)
rates.append(7)
rates.append(10)
rates.append(20)
rates.append(40)
rates.append(70)
rates.append(100)
rates.append(200)
rates.append(400)
rates.append(700)
rates.append(1000)

for i in rates:
    rate_ids[spec_rate * i] = rate_id
    rate_id += 1


exp_name = 'for_paper_flat_20161026'
router_name = 'router_amf'
start_round = 0
end_round = 5

for dir in range(start_round, end_round + 1):
    dir_path = '/home/haowu11/scratch/large-flow/data/exp_logger/' \
        + exp_name + '/traffic_and_damage/' + router_name + '/' + str(dir)
    for fname in os.listdir(dir_path):
        path = dir_path + '/' + fname
        count = 0
        with open(path, 'r') as f:
            for line in f:
                count += 1
        if count < num_counter + 1:
            print path + '\t lines = ' + str(count)
            rate = int(fname.split('.')[0])
            rate_id = rate_ids[rate]
            job_file_path = '/home/haowu11/large-flow/job_scripts/'\
                + exp_name + '/round-' + str(dir)\
                + '-rate-' + str(rate_id) + '.pbs'
            print job_file_path
            call(['qsub', job_file_path])

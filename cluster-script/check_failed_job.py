import os
from subprocess import call

rate_ids = dict()
spec_rate = 50000
rate_id = 0
rates = range(1, 10)
rates.extend(range(10, 100, 10))
rates.extend(range(100, 1001, 100))
for i in rates:
    rate_ids[spec_rate * i] = rate_id
    rate_id += 1


exp_name = 'packet_loss_hybrid_long_test_burst_20160911'
router_name = 'router_amf'
start_round = 0
end_round = 4

for dir in range(start_round, end_round + 1):
    dir_path = '/home/haowu11/scratch/large-flow/data/exp_logger/' \
        + exp_name + '/traffic_and_damage/' + router_name + '/' + str(dir)
    for fname in os.listdir(dir_path):
        path = dir_path + '/' + fname
        count = 0
        with open(path, 'r') as f:
            for line in f:
                count += 1
        if count < 11:
            print path + '\t lines = ' + str(count)
            rate = int(fname.split('.')[0])
            rate_id = rate_ids[rate]
            job_file_path = '/home/haowu11/large-flow/job_scripts/'\
                + exp_name + '/round-' + str(dir)\
                + '-rate-' + str(rate_id) + '.pbs'
            print job_file_path
            call(['qsub', job_file_path])

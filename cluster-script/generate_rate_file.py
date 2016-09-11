import os

dir_path = './rate_1_in_1'

if not os.path.exists(dir_path):
    os.makedirs(dir_path)

rates = range(1, 10)
rates.extend(range(10, 100, 10))
rates.extend(range(100, 1001, 100))

num_rate_per_file = 1

i = 0   # file index
rr = 0  # next rate index
while rr < len(rates):
    with open(dir_path + '/rate-' + str(i) + '.txt', 'w') as out:
        rr_end = min(len(rates), rr + num_rate_per_file)
        for k in range(rr, rr_end):
            out.write(str(rates[k]) + '\n')
    rr = rr_end
    i += 1

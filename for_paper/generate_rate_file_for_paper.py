import os

dir_path = './for_paper_rate_1_in_1'

if not os.path.exists(dir_path):
    os.makedirs(dir_path)

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

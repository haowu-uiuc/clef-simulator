import os
import time

startRound = 0
numOfRounds = 20
expName = "full_link_base_20170208"
jarName = "Main_MaxPacketLossEvaluation_configurable.jar"
scratchExpDir = "/home/haowu11/scratch/large-flow"
counter_file_name = "counter_base.txt"
config_file_name = expName + ".json"
rate_file_names = ["rate-0.txt", "rate-1.txt", "rate-2.txt"]

rate_dir = None
rate_dir = 'for_paper_rate_1_in_1'
if rate_dir is not None:
    rate_file_names = []
    for file in os.listdir('atk_rate/' + rate_dir):
        rate_file_names.append(file)
        # for baseline we only need to test on a (any) rate,
        # as no attack flows
        break

scriptDir = "./job_scripts/" + expName
if not os.path.exists(scriptDir):
    os.makedirs(scriptDir)

if not os.path.exists(scratchExpDir):
    os.makedirs(scratchExpDir)

pbs_prefix_template = """\
#!/bin/bash

#
#PBS -l walltime=04:00:00
#PBS -l nodes=1:ppn=1
#PBS -N LFJ-{date}
#PBS -q secondary
#PBS -j oe
###PBS -m be
###PBS -o large-flow-job.out
###PBS -e large-flow-job.err
###PBS -m be
#
#####################################

""".format(date=time.strftime("%x"))

pbs_template = """\

# Change to the directory from which the batch job was submitted
cd {scratch_exp_dir}

# Assigned the numeric portion of the pbs jobid to a varaible
export JOBID=`echo $PBS_JOBID | cut -d"." -f1`

# Load JAVA 1.8 module
module load java/1.8

# Run JAVA code
java -Xms1024m -Xmx2048m -d64 -jar {jar_name}""".format(
    jar_name=jarName,
    scratch_exp_dir=scratchExpDir)

shell_file = open(scriptDir + "/run_jobs.sh", "w")

# copy the jar and input data into scratch dir
cmd = """\
cp /home/haowu11/large-flow/{jar_name} {scratch_exp_dir}/
mkdir -p {scratch_exp_dir}/data
rm -r {scratch_exp_dir}/data/real_traffic
cp -r /home/haowu11/large-flow/data/real_traffic {scratch_exp_dir}/data
rm -r {scratch_exp_dir}/atk_rate
cp -r /home/haowu11/large-flow/atk_rate {scratch_exp_dir}
rm -r {scratch_exp_dir}/counter
cp -r /home/haowu11/large-flow/counter {scratch_exp_dir}
rm -r {scratch_exp_dir}/config
cp -r /home/haowu11/large-flow/config {scratch_exp_dir}
""".format(
    jar_name=jarName,
    scratch_exp_dir=scratchExpDir)

shell_file.write(cmd)

for i in range(startRound, numOfRounds + startRound):
    for rate_file_name in rate_file_names:
    	tmp_strs = rate_file_name.split('/')
        rate_file_prefix = tmp_strs[len(tmp_strs)-1].split('.')[0]
        pbs_file_name = "round-" + str(i) + "-" + \
            rate_file_prefix + ".pbs"

        f = open(scriptDir + "/" + pbs_file_name, "w")
        f.write(pbs_prefix_template)
        f.write("echo " + pbs_file_name + "\n")
        f.write(pbs_template)
        f.write(" --start_round " + str(i))
        f.write(" --repeat_rounds 1")
        if rate_dir is None:
            f.write(" --rate atk_rate/" + rate_file_name)
        else:
            f.write(" --rate atk_rate/" + rate_dir + '/' + rate_file_name)
        f.write(" --counter counter/" + counter_file_name)
        f.write(" --config config/" + config_file_name)
        f.close()
        shell_file.write("qsub ./" + pbs_file_name + "\n")

shell_file.close()

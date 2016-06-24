import os

startRound = 0
numOfRounds = 10
expName = "packet_loss_burst_20160612"
jarName = "Main_MaxPacketLossEvaluation_burst_20160612.jar"
scratchExpDir = "/home/haowu11/scratch/large-flow"
counter_file_name = "counter.txt"

scriptDir = "./job_scripts/" + expName
if not os.path.exists(scriptDir):
    os.makedirs(scriptDir)

if not os.path.exists(scratchExpDir):
    os.makedirs(scratchExpDir)

pbs_template = """\
#!/bin/bash

#
#PBS -l walltime=04:00:00
#PBS -l nodes=1:ppn=1
#PBS -N LFJ-{exp_name}
#PBS -q secondary
#PBS -j oe
###PBS -m be
###PBS -o large-flow-job.out
###PBS -e large-flow-job.err
###PBS -m be
#
#####################################

# Change to the directory from which the batch job was submitted
cd {scratch_exp_dir}

# Assigned the numeric portion of the pbs jobid to a varaible
export JOBID=`echo $PBS_JOBID | cut -d"." -f1`

# Load JAVA 1.8 module
module load java/1.8

# Run JAVA code
java -Xms1024m -Xmx2048m -d64 -jar {jar_name} {exp_name} """.format(
	exp_name=expName,
	jar_name=jarName,
	scratch_exp_dir=scratchExpDir)

shell_file = open(scriptDir + "/run_jobs.sh", "w")

# copy the jar and input data into scratch dir
cmd = """\
cp /home/haowu11/large-flow/{jar_name} {scratch_exp_dir}/
mkdir -p {scratch_exp_dir}/data
cp -r /home/haowu11/large-flow/data/real_traffic {scratch_exp_dir}/data
cp -r /home/haowu11/large-flow/atk_rate {scratch_exp_dir}
cp -r /home/haowu11/large-flow/counter {scratch_exp_dir}
""".format(
	jar_name=jarName,
	scratch_exp_dir=scratchExpDir)

shell_file.write(cmd);

for i in range(startRound, numOfRounds + startRound):
	for j in range(0, 3):
		f = open(scriptDir + "/round-" + str(i)
			+ "-rate-" + str(j) + ".pbs", "w")
		f.write(pbs_template)
		f.write(str(i) + " 1")
		f.write(" --rate atk_rate/rate-" + str(j) + ".txt")
		f.write(" --counter counter/" + counter_file_name)
		f.close()
		shell_file.write("qsub ./round-" + str(i)
			+ "-rate-" + str(j) + ".pbs\n")

shell_file.close()

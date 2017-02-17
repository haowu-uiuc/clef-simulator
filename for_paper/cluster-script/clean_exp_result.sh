expname="$1"
echo cleaning exp $expname
rm -r ~/scratch/large-flow/data/exp_logger/$expname/trace/
cp -r ~/scratch/large-flow/data/exp_logger/$expname/ ~/large-flow/data/exp_logger/

module load java/1.8
java -jar ~/large-flow/Main_LogParser.jar -n $expname
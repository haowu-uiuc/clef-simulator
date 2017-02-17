expname="$1"
dstdir="$2"
echo pulling exp $expname

echo making dir $dstdir/$expname
mkdir $dstdir/$expname
scp $CC_HOME/large-flow/data/exp_logger/$expname/*.* $dstdir/$expname/
import random;
import sys;

endTime = float(sys.argv[1]);
inFile = open(sys.argv[2]);
listOfAllTheLines = inFile.readlines();
outFile = open('flowSizeStatistics_Total.txt', 'w');



totalSize = 0;
flowIdToSizeDict = dict();

count = 0;
for i in range(0, len(listOfAllTheLines)):
    list = listOfAllTheLines[i].split();
    flowId = list[0];
    pktSize = int(list[1]);
    time = float(list[2]);
    count = count + 1;
    
    if time > endTime:
        break;
    if flowIdToSizeDict.has_key(flowId) == False:
        flowIdToSizeDict[flowId] = pktSize;
    else:
        flowIdToSizeDict[flowId] = flowIdToSizeDict[flowId] + pktSize;    

    totalSize = totalSize + pktSize;
    outFile.write(str(count) + ' ' + str(totalSize) + ' ' + str(time) + '\n');




inFile.close;
outFile.close;

outFile = open('flowSizeStatistics_Each.txt', 'w');
it = iter(flowIdToSizeDict);

count = 0;
while 1:
    try:
        count = count + 1;
        x = it.next();
        outFile.write(str(count) + ' ' + str(x) + ' ' + str(flowIdToSizeDict[x]) + '\n');
    except StopIteration:
        break;











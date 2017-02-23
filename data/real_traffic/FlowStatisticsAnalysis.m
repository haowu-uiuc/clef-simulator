close all, clear, clc;

FlowSize_Total = load('./flowSizeStatistics_Total.txt');
FlowSize_Each = load('./flowSizeStatistics_Each.txt');
RealTrace = load('./RealTrafficFlowGeneratorFlows.txt');

totalSize = FlowSize_Total(:,2);
totalTime = FlowSize_Total(:,3);
totalRate = (totalSize(1:length(totalSize)-1) - [0;totalSize(1:length(totalSize)-2)])./...
            (totalTime(2:length(totalTime)) - totalTime(1:length(totalTime)-1));
eachSize = FlowSize_Each(:,3);

minPktSize = min(RealTrace(:,2))
maxPktSize = max(RealTrace(:,2))
maxRate = max(totalRate)
averageRate = max(totalSize) / max(totalTime)
numOfFlows = size(FlowSize_Each, 1)

figure;
title('Size');
plot(totalTime, totalSize, 'b');
figure;
title('Rate');
plot(totalTime(1:length(totalTime)-1), totalRate, 'b');
figure;
hist(eachSize,1000);
            
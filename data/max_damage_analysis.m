clear all, close all, clc;

loadDataFromFile = 0;
expName = '20151018_MaxDamageEvaluator_2';
detectorNameList = {'eardet', 'egregious-detector'};
numTF = 6150; % not quite true but we can approximately treat the TF as this.
numTP = 10;
drawRatio = true;

for i = 1:length(detectorNameList)
    detectorName = detectorNameList{i};
    if loadDataFromFile
        load(['./exp_logger/', expName, '/', detectorName, '.mat']);
    else
        fid= fopen(['./exp_logger/', expName, '/', detectorName, '.txt']);

        fgetl(fid); %skip the first title line 
        tline = fgetl(fid);

        rateToIndexMap = containers.Map('KeyType', 'int32','ValueType', 'int32');
        counterToIndexMap = containers.Map('KeyType', 'int32','ValueType', 'int32');

        rateList = [];
        counterList = [];
        damageMatrix = [];
        FPMatrix = [];
        TPMatrix = [];
        FNMatrix = [];

        rateIndex = 0;
        counterIndex = 0;

        round = 0;
        while ischar(tline)

            strCells = strsplit(tline, ' ');
            len = length(strCells);
            if len == 3 && strcmp(strCells{1}, 'Round') && strcmp(strCells{2}, '#')
                round = str2num(strCells{3})
                tline = fgetl(fid);
                continue;
            end

            strCells = strsplit(tline, '\t');
            atkRate = str2num(strCells{1});
            numOfCounter = str2num(strCells{2});
            FP = str2num(strCells{3});
            FN = str2num(strCells{4});
            TP = str2num(strCells{5});
            damage = -Inf;
            if strcmp(strCells{6}, 'Infinity');
                damage = Inf;
            elseif strcmp(strCells{6}, '-Infinity');
                damage = 0;
            else
                damage = str2num(strCells{6});
                if damage < 0
                    damage = 0;
                end
            end

            if ~rateToIndexMap.isKey(atkRate)
                rateIndex = rateIndex + 1;
                rateToIndexMap(atkRate) = rateIndex;
                rateList(rateIndex) = atkRate;
            end

            if ~counterToIndexMap.isKey(numOfCounter)
                counterIndex = counterIndex + 1;
                counterToIndexMap(numOfCounter) = counterIndex;
                counterList(counterIndex) = numOfCounter;
            end

            curRateIndex = rateToIndexMap(atkRate);
            curCounterIndex = counterToIndexMap(numOfCounter);
            if round == 0
                damageMatrix(curRateIndex, curCounterIndex) = damage;
                FPMatrix(curRateIndex, curCounterIndex) = FP;
                TPMatrix(curRateIndex, curCounterIndex) = TP;
                FNMatrix(curRateIndex, curCounterIndex) = FN;
            else
                damageMatrix(curRateIndex, curCounterIndex) = ...
                    damageMatrix(curRateIndex, curCounterIndex) + damage;
                FPMatrix(curRateIndex, curCounterIndex) = ...
                    FPMatrix(curRateIndex, curCounterIndex) + FP;
                TPMatrix(curRateIndex, curCounterIndex) = ...
                    TPMatrix(curRateIndex, curCounterIndex) + TP;
                FNMatrix(curRateIndex, curCounterIndex) = ...
                    FNMatrix(curRateIndex, curCounterIndex) + FN;
            end

            tline = fgetl(fid);
        end
        % average the damage by rounds
        damageMatrix = damageMatrix ./ (round + 1);
        
        save(['./exp_logger/', expName, '/', detectorName, '.mat'], 'damageMatrix', 'rateList', 'counterList', ...
            'FPMatrix', 'TPMatrix', 'FNMatrix', 'round');
    end

    numOfRateSample = size(damageMatrix, 1);
    numOfCounterSample = size(damageMatrix, 2);
    
    if drawRatio
        FPMatrix = FPMatrix ./ numTF / (round + 1);
        FNMatrix = FNMatrix ./ numTP / (round + 1);
    end
        
    lineWidth = 1;
    if numOfRateSample > 1 && numOfCounterSample > 1
        % get mesh grid
        [X, Y] = meshgrid(counterList, rateList);
    
        fig = figure;
        contourf(X, Y, damageMatrix, 'LineWidth',lineWidth);
        colorbar();
        title(['Per-flow Damage of ', detectorName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-damage.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, FPMatrix, 'LineWidth',lineWidth);
        colorbar();
        title(['Sum of FP of ', detectorName]);
        if drawRatio
            title(['Ratio of FP of ', detectorName]);
        end
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-FP.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, TPMatrix, 'LineWidth',lineWidth);
        colorbar();
        title(['Sum of TP of ', detectorName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-TP.eps'], 'epsc');
        
        fig = figure;
        contourf(X, Y, FNMatrix, 'LineWidth',lineWidth);
        colorbar();
        title(['Sum of FN of ', detectorName]);
        if drawRatio
            title(['Ratio of FN of ', detectorName]);
        end
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-FN.eps'], 'epsc');
    elseif numOfRateSample > 1 && numOfCounterSample == 1
        fig = figure;
        plot(rateList, damageMatrix(:, 1), '-*');
        title(['Per-flow Damage of ', detectorName]);
        xlabel('Large Flow Rate');
        ylabel('Per-flow Damage');
        h = max(damageMatrix) - min(damageMatrix);
        axis([min(rateList), max(rateList), min(damageMatrix) - 0.1 * h, max(damageMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-damage.eps'], 'epsc');

        fig = figure;
        plot(rateList, FPMatrix(:, 1), '-*');
        title(['Sum of FP of ', detectorName]);
        if drawRatio
            title(['Ratio of FP of ', detectorName]);
        end
        xlabel('Large Flow Rate');
        ylabel('FP');
        h = max(FPMatrix) - min(FPMatrix);
        axis([min(rateList), max(rateList), min(FPMatrix) - 0.1 * h, max(FPMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-FP.eps'], 'epsc');

        fig = figure;        
        plot(rateList, TPMatrix(:, 1), '-*');
        title(['Sum of TP of ', detectorName]);
        xlabel('Large Flow Rate');
        ylabel('TP');
        h = max(TPMatrix) - min(TPMatrix);
        axis([min(rateList), max(rateList), min(TPMatrix) - 0.1 * h, max(TPMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-TP.eps'], 'epsc');

        fig = figure;
        plot(rateList, FNMatrix(:, 1), '-*');
        title(['Sum of FN of ', detectorName]);
        if drawRatio
            title(['Ratio of FN of ', detectorName]);
        end
        xlabel('Large Flow Rate');
        ylabel('FN');
        h = max(FNMatrix) - min(FNMatrix);
        axis([min(rateList), max(rateList), min(FNMatrix) - 0.1 * h, max(FNMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-FN.eps'], 'epsc');

    elseif numOfCounterSample > 1 && numOfRateSample == 1
        fig = figure;
        plot(counterList, damageMatrix(:, 1), '-*');
        title(['Per-flow Damage of ', detectorName]);
        xlabel('Counter Number');
        ylabel('Per-flow Damage')
        h = max(damageMatrix) - min(damageMatrix);
        axis([min(counterList), max(counterList), min(damageMatrix) - 0.1 * h, max(damageMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-damage.eps'], 'epsc');

        fig = figure;
        plot(counterList, FPMatrix(:, 1), '-*');
        title(['Sum of FP of ', detectorName]);
        if drawRatio
            title(['Ratio of FP of ', detectorName]);
        end
        xlabel('Counter Number');
        ylabel('FP');
        h = max(FPMatrix) - min(FPMatrix);
        axis([min(counterList), max(counterList), min(FPMatrix) - 0.1 * h, max(FPMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-FP.eps'], 'epsc');

        fig = figure;        
        plot(counterList, TPMatrix(:, 1), '-*');
        title(['Sum of TP of ', detectorName]);
        xlabel('Counter Number');
        ylabel('TP');
        h = max(TPMatrix) - min(TPMatrix);
        axis([min(counterList), max(counterList), min(TPMatrix) - 0.1 * h, max(TPMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-TP.eps'], 'epsc');

        fig = figure;
        plot(counterList, FNMatrix(:, 1), '-*');
        title(['Sum of FN of ', detectorName]);
        if drawRatio
            title(['Ratio of FN of ', detectorName]);
        end
        xlabel('Counter Number');
        ylabel('FN');
        h = max(FNMatrix) - min(FNMatrix);
        axis([min(counterList), max(counterList), min(FNMatrix) - 0.1 * h, max(FNMatrix) + 0.1 * h]);
        saveas(fig, ['./exp_logger/', expName, '/', detectorName, '-FN.eps'], 'epsc');
        
    else
        display('You need more samples on rate or counter!');
    end
end
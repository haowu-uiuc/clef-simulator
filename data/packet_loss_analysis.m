clear all, close all, clc;

loadDataFromFile = 0;
% expName = 'packet_loss_burst_20160421_2';
% expName = 'packet_loss_burst_20160801';
% expName = 'packet_loss_config_test_20160906_2';
% expName = 'packet_loss_hybrid_test_20160911';
% expName = 'packet_loss_hybrid_long_test_20160911'
% expName = 'packet_loss_hybrid_least_value_eviction_20160916';
% expName = 'packet_loss_hybrid_small_packet_20160917';
expName = 'packet_loss_hybrid_long_test_burst_20160911';
expName = 'packet_loss_fmd_20160919'
% expName = 'packet_loss_flat_20160801_amf_test_fixed_limited_size'
% expName = 'test_packet_loss_exp';
routerNameList = {'router_eardet', 'router_eg', 'router_fmf', 'router_amf', 'router_eardet_efd'};
routerNameList = {'router_fmf', 'router_amf', 'router_fmd'};
% routerNameList = {'router_fmf_lbve', 'router_amf_lbve'};
% routerNameList = {'router_eardet', 'router_eg', 'router_fmf', 'router_amf', 'router_eardet_efd', 'router_fmf_lbve', 'router_amf_lbve'};
% routerNameList = {'router_eardet', 'router_eg', 'router_amf'};
% routerNameList = {'router_fmf', 'router_amf'};

routerLabelList = {'EARDet', 'EFD', 'FMF w/ FM', 'AMF w/ FM', 'EARDet w/ EFD' };
routerLabelList = {'FMF w/ FM', 'AMF w/ FM', 'FMD' };
% routerLabelList = {'EARDet', 'EFD', 'AMF w/ FM'};
% routerLabelList = {'FMF w/ FM (LBVE)', 'AMF w/ FM (LBVE)'};
% routerLabelList = {'EARDet', 'EFD', 'FMF w/ FM', 'AMF w/ FM', 'EARDet w/ EFD', 'FMF w/ FM (LBVE)', 'AMF w/ FM (LBVE)'};

drawRatio = false;
minRate = 0;
maxRate = 50000000;
start_round = 0;
draw_heat_map = true;  % 1 for ture, 0 for false

total_damageMatrix_list = {};
FNMatrix = {};

for i = 1:length(routerNameList)
    routerName = routerNameList{i};
    
    if loadDataFromFile
        load(['./exp_logger/', expName, '/matlab_data/', routerName, '.mat']);
    else
        fid= fopen(['./exp_logger/', expName, '/total_', routerName, '_traffic_damage.txt']);

        fgetl(fid); %skip the first title line 
        tline = fgetl(fid);

        rateToIndexMap = containers.Map('KeyType', 'int32','ValueType', 'int32');
        counterToIndexMap = containers.Map('KeyType', 'int32','ValueType', 'int32');

        rateList = [];
        counterList = [];
        total_damageMatrix = [];
        be_damageMatrix = [];
        fp_damageMatrix = [];
        qd_damageMatrix = [];
        FPMatrix = [];
        TPMatrix = [];
        FNMatrix = [];

        rateIndex = 0;
        counterIndex = 0;

        round = 0;
        while ischar(tline)

            strCells = strsplit(tline, '\t');
            len = length(strCells);
            round = str2num(strCells{1});
            atkRate = str2num(strCells{2});
            numOfCounter = str2num(strCells{3});
            FP = str2num(strCells{10});
            FN = str2num(strCells{11});
            TP = str2num(strCells{12});
            total_damage = str2num(strCells{14});
            be_damage = str2num(strCells{15});
            fp_damage = str2num(strCells{16});
            qd_damage = str2num(strCells{17});
            
            
            % filter out rate out of the range [minRate, maxRate]
            if (atkRate > maxRate || atkRate < minRate)
                tline = fgetl(fid);
                continue;
            end
            
            if (round == start_round)
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
            end

            curRateIndex = rateToIndexMap(atkRate);
            curCounterIndex = counterToIndexMap(numOfCounter);
            if round == start_round
                total_damageMatrix(curRateIndex, curCounterIndex) = total_damage;
                be_damageMatrix(curRateIndex, curCounterIndex) = be_damage;
                fp_damageMatrix(curRateIndex, curCounterIndex) = fp_damage;
                qd_damageMatrix(curRateIndex, curCounterIndex) = qd_damage;
                FPMatrix(curRateIndex, curCounterIndex) = FP;
                TPMatrix(curRateIndex, curCounterIndex) = TP;
                FNMatrix(curRateIndex, curCounterIndex) = FN;
            else
                total_damageMatrix(curRateIndex, curCounterIndex) = ...
                    total_damageMatrix(curRateIndex, curCounterIndex) + total_damage;
                be_damageMatrix(curRateIndex, curCounterIndex) = ...
                    be_damageMatrix(curRateIndex, curCounterIndex) + be_damage;
                fp_damageMatrix(curRateIndex, curCounterIndex) = ...
                    fp_damageMatrix(curRateIndex, curCounterIndex) + fp_damage;
                qd_damageMatrix(curRateIndex, curCounterIndex) = ...
                    qd_damageMatrix(curRateIndex, curCounterIndex) + qd_damage;
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
        total_damageMatrix = total_damageMatrix ./ (round + 1 - start_round);
        be_damageMatrix = be_damageMatrix ./ (round + 1 - start_round);
        fp_damageMatrix = fp_damageMatrix ./ (round + 1 - start_round);
        qd_damageMatrix = qd_damageMatrix ./ (round + 1 - start_round);
        FPMatrix = FPMatrix ./ (round + 1);
        TPMatrix = TPMatrix ./ (round + 1);
        FNMatrix = FNMatrix ./ (round + 1);

        
        data_dir = ['./exp_logger/', expName, '/matlab_data/'];
        mkdir(data_dir);
        save([data_dir, routerName, '.mat'], ...
            'total_damageMatrix', 'be_damageMatrix', ...
            'fp_damageMatrix', 'qd_damageMatrix', ...
            'rateList', 'counterList', ...
            'FPMatrix', 'TPMatrix', 'FNMatrix', 'round');
    end
    
    total_damageMatrix_list{i} = total_damageMatrix;
    FNMatrix_list{i} = FNMatrix;
    
    fig_dir = ['./exp_logger/', expName, '/fig/'];
    mkdir(fig_dir);
    
    if draw_heat_map
        lineWidth = 0;
        % get mesh grid
        [X, Y] = meshgrid(counterList, rateList);

        n = 40;
        cmap = hot;
        default_constant_color = 'k';

        fig = figure;
        contourf(X, Y, total_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'color',default_constant_color)
        colorbar();
        title(['Total Damage of ', routerName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-total-damage.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, be_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'color',default_constant_color)
        colorbar();
        title(['Best-Effort Damage of ', routerName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-be-damage.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, fp_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'color',default_constant_color)
        colorbar();
        title(['FP Damage of ', routerName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-fp-damage.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, qd_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'color',default_constant_color)
        colorbar();
        title(['QD drop Damage of ', routerName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-qd-damage.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, FPMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);  
        set(gca,'color',default_constant_color)
        colorbar();
        title(['Sum of FP of ', routerName]);
        %         if drawRatio
        %             title(['Ratio of FP of ', detectorName]);
        %         end
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-FP.eps'], 'epsc');

        % TODO: Fix bug to draw the figure with value of TP
        fig = figure;
        contourf(X, Y, TPMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap); 
        set(gca,'color',default_constant_color)
        colorbar();
        title(['Sum of TP of ', routerName]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-TP.eps'], 'epsc');

        fig = figure;
        contourf(X, Y, FNMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap); 
        set(gca,'color',default_constant_color)
        colorbar();
        title(['Sum of FN of ', routerName]);
        %         if drawRatio
        %             title(['Ratio of FN of ', detectorName]);
        %         end
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-FN.eps'], 'epsc');
    end
end

% draw damage comparison figure at a # of counter
fig_num_counters = [60, 100, 160, 200];
colors = {'b-*', 'r-x', 'm-v', 'k-^', 'g-d', 'c-o', 'y-+'};
for j = 1:length(fig_num_counters)
    fig_num_counter = fig_num_counters(j);
    counterIndex = counterToIndexMap(fig_num_counter);
    fig = figure;
    for i = 1:length(routerNameList)
        total_damageMatrix = total_damageMatrix_list{i};
        semilogx(rateList, total_damageMatrix(:, counterIndex), colors{i});
        hold on;
    end
    title(['Total Damage when counter number is ', num2str(fig_num_counter)]);
    xlabel('Large Flow Rate (Bytes / second)')
    ylabel('Total Damage');
    ylim([0, 500000000]);
    legend(routerLabelList);
    saveas(fig, [fig_dir, 'total_damage_at_counter_', num2str(fig_num_counter), '.pdf'], 'pdf');
end

for j = 1:length(fig_num_counters)
    fig_num_counter = fig_num_counters(j);
    counterIndex = counterToIndexMap(fig_num_counter);
    fig = figure;
    for i = 1:length(routerNameList)
        FNMatrix = FNMatrix_list{i};
        semilogx(rateList, FNMatrix(:, counterIndex), colors{i});
        hold on;
    end
    title(['FN when counter number is ', num2str(fig_num_counter)]);
    xlabel('Large Flow Rate (Bytes / second)')
    ylabel('FN');
    legend(routerLabelList);
    saveas(fig, [fig_dir, 'FN_at_counter_', num2str(fig_num_counter), '.pdf'], 'pdf');
end

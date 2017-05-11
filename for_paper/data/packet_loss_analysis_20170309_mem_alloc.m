clear all, close all, clc;

loadDataFromFile = 0;
exp_dir = 'exp_logger_mem_alloc';

% eardet : rlfd
% fm : amf
mem_prefix = {'mem_10-90_', 'mem_30-70_', ...
    'mem_50-50_', 'mem_70-30_', 'mem_90-10_'};
legend_str = {'\phi = 10:90', '\phi = 30:70',...
    '\phi = 50:50', '\phi = 70:30', '\phi = 90:10'};
exp_suffix = {'flat_20170311', 'burst_4_20170311', ...
    'burst_2_20170311', 'burst_1_20170311', 'burst_5_20170311'}';
% exp_suffix = {'flat_20170311'};
thetas = [1.0, 0.5, 0.25, 0.1, 0.02];
damage_ylim = [3*10^8, 3*10^8, 3*10^8, 3*10^8, 3*10^8];
max_damages = [5*10^8, 5*10^8, 5*10^8, 5*10^8, 5*10^8];
fig_num_counters = [200];
routerNameList = {'router_amf', 'router_eardet_efd'};
efd_eardet_index = 2;
routerLabelList = {'AMF-FM', 'CLEF' };

drawRatio = false;
minRate = 0;
maxRate = 12500000;
start_round = 0;
draw_heat_map = false;  % 1 for ture, 0 for false
draw_total_damage = false;
draw_be_damage = false;
draw_fp_damage = false;
draw_qd_damage = false;
draw_fp = false;
draw_fn = false;
draw_tp = false;

all_total_damageMatrix_list = {};
all_FNMatrix_list = {};

% TODO: change exp_idx
for theta_idx = 1:length(exp_suffix)

for mem_idx = 1:length(mem_prefix)

expName = [mem_prefix{mem_idx}, exp_suffix{theta_idx}];
    
total_damageMatrix_list = {};
FNMatrix_list = {};

for i = 1:length(routerNameList)
    routerName = routerNameList{i};
    routerLabel = routerLabelList{i};
    
    if loadDataFromFile
        load(['./', exp_dir,'/', expName, '/matlab_data/', routerName, '.mat']);
    else
        fid= fopen(['./', exp_dir,'/', expName, '/total_', routerName, '_traffic_damage.txt']);

        fgetl(fid); %skip the first title line 
        tline = fgetl(fid);

        rateToIndexMap = containers.Map('KeyType', 'int32','ValueType', 'int32');
        counterToIndexMap{mem_idx} = containers.Map('KeyType', 'int32','ValueType', 'int32');

        rateList = [];
        counterList = [];
        total_damageMatrix = [];
        be_damageMatrix = [];
        fp_damageMatrix = [];
        qd_damageMatrix = [];
        baseline_damageMatrix = [];
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
            baseline_damage = str2num(strCells{18});
            
            
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

                if ~counterToIndexMap{mem_idx}.isKey(numOfCounter)
                    counterIndex = counterIndex + 1;
                    counterToIndexMap{mem_idx}(numOfCounter) = counterIndex;
                    counterList(counterIndex) = numOfCounter;
                end
            end

            curRateIndex = rateToIndexMap(atkRate);
            curCounterIndex = counterToIndexMap{mem_idx}(numOfCounter);
            if round == start_round
                total_damageMatrix(curRateIndex, curCounterIndex) = total_damage;
                be_damageMatrix(curRateIndex, curCounterIndex) = be_damage;
                fp_damageMatrix(curRateIndex, curCounterIndex) = fp_damage;
                qd_damageMatrix(curRateIndex, curCounterIndex) = qd_damage;
                baseline_damageMatrix(curRateIndex, curCounterIndex) = baseline_damage;
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
                baseline_damageMatrix(curRateIndex, curCounterIndex) = ...
                    baseline_damageMatrix(curRateIndex, curCounterIndex) + baseline_damage;
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
        baseline_damageMatrix = baseline_damageMatrix ./ (round + 1 - start_round);
        FPMatrix = FPMatrix ./ (round + 1);
        TPMatrix = TPMatrix ./ (round + 1);
        FNMatrix = FNMatrix ./ (round + 1);

        
        data_dir = ['./', exp_dir, '/', expName, '/matlab_data/'];
        mkdir(data_dir);
        save([data_dir, routerName, '.mat'], ...
            'total_damageMatrix', 'be_damageMatrix', ...
            'fp_damageMatrix', 'qd_damageMatrix', ...
            'rateList', 'counterList', ...
            'FPMatrix', 'TPMatrix', 'FNMatrix', 'round',...
            'counterToIndexMap', 'rateToIndexMap');
    end
    
%     total_damageMatrix = be_damageMatrix + qd_damageMatrix; %%%%% a hack %%%%%
    % if we don't consider be damage:
    z = zeros(size(total_damageMatrix));
    total_damageMatrix = max(qd_damageMatrix + fp_damageMatrix - baseline_damageMatrix, z);
    total_damageMatrix_list{i} = total_damageMatrix;
    FNMatrix_list{i} = FNMatrix;    
end


all_total_damageMatrix_list{mem_idx} = total_damageMatrix_list;
all_FNMatrix_list{mem_idx} = FNMatrix_list;

end


% draw figures across for efd-eardet
ee_fig_dir = ['./', exp_dir, '/fig/', exp_suffix{theta_idx}, '/'];
mkdir(ee_fig_dir);
% fig_num_counters = [40, 100, 200, 400];
colors = {'b-*', 'r-x', 'm-v', 'k-^', 'g-d', 'c-o', 'y-+'};
for k = 1:length(routerLabelList)
    routerLabel = routerLabelList{k};
    routerName = routerNameList{k};
for j = 1:length(fig_num_counters)
    fig_num_counter = fig_num_counters(j);
    fig = figure;
    for i = 1:length(mem_prefix)
        counterIndex = counterToIndexMap{i}(fig_num_counter);
        total_damageMatrix = all_total_damageMatrix_list{i}{k};
        semilogx(rateList, total_damageMatrix(:, counterIndex), colors{i});
        hold on;
%         legend_str{i} = mem_prefix{i};
    end
    title([routerLabel, ', m=', num2str(fig_num_counter), ' \theta=', num2str(thetas(theta_idx))]);
    xlabel('Large Flow Rate (B/s)');
    ylabel('Damage (Byte)');
    legend(legend_str)
%     ylim([10^6, 5000000000]);
    ylim([0, 1.2*10^8]);
    set(gcf,'Position',[100 100 250 150]);
    file_path = [ee_fig_dir, routerName, '_total_damage_at_counter_', num2str(fig_num_counter)];
    saveas(fig, [file_path, '.pdf'], 'pdf');
    
    fig = figure;
    maxFN = 10;
    for i = 1:length(mem_prefix)
        counterIndex = counterToIndexMap{i}(fig_num_counter);
        FNMatrix = all_FNMatrix_list{i}{k};
        if thetas(i) < 1.0
            semilogx(rateList, FNMatrix(:, counterIndex)/maxFN, colors{i});
        else
            tmp = FNMatrix(:, counterIndex);
            semilogx(rateList, [10; FNMatrix(2:length(tmp), counterIndex)]/maxFN, colors{i});
        end
        hold on;
%         legend_str{i} = mem_prefix{i};
    end
    title([routerLabel, ', m=', num2str(fig_num_counter), ' \theta=', num2str(thetas(theta_idx))]);
    xlabel('Large Flow Rate (B/s)');
    ylabel('FN Ratio');
    legend(legend_str)
    set(gcf,'Position',[100 100 250 150]);
    file_path = [ee_fig_dir, routerName, '_FN_at_counter_', num2str(fig_num_counter)];
    saveas(fig, [file_path '.pdf'], 'pdf');
end

end

end

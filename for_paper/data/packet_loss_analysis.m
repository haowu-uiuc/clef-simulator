clear all, close all, clc;

loadDataFromFile = 1;
expNames = {'for_paper_flat_20161026', 'for_paper_burst_1_20161026',...
    'for_paper_burst_2_20161026', 'for_paper_burst_4_20161026',...
    'for_paper_burst_3_20161026'};
expNames = {'for_paper_flat_20161026',...
    'for_paper_burst_4_20161026', 'for_paper_burst_2_20161026',...
    'for_paper_burst_1_20161026', 'for_paper_burst_5_20161026'};
% expNames = {'for_paper_flat_20161026'};
% thetas = [1.0, 0.1, 0.2, 0.5, 0.8];
% thetas = [1.0, 0.1, 0.25, 0.5, 0.75];
thetas = [1.0, 0.5, 0.25, 0.1, 0.02];
damage_ylim = [3*10^8, 3*10^8, 3*10^8, 3*10^8, 3*10^8];
max_damages = [5*10^8, 5*10^8, 5*10^8, 5*10^8, 5*10^8];
fig_num_counters = [40, 100, 200, 400];
fig_num_counters = [200];
% expName = 'for_paper_flat_20161026';
% expName = 'for_paper_burst_1_20161026'
% expName = 'for_paper_burst_2_20161026'
% expName = 'for_paper_burst_3_20161026'
% expName = 'for_paper_burst_4_20161026'
routerNameList = {'router_eardet', 'router_eg', 'router_amf', 'router_eardet_efd'};
efd_eardet_index = 4;

routerLabelList = {'EARDet', 'RLFD', 'AMF-FM', 'CLEF' };

drawRatio = false;
minRate = 0;
maxRate = 12500000;
start_round = 0;
draw_heat_map = true;  % 1 for ture, 0 for false
draw_total_damage = true;
draw_be_damage = false;
draw_fp_damage = false;
draw_qd_damage = false;
draw_fp = false;
draw_fn = false;
draw_tp = false;

all_total_damageMatrix_list = {};
all_FNMatrix_list = {};

for exp_idx = 1:length(expNames)

expName = expNames{exp_idx};
    
total_damageMatrix_list = {};
FNMatrix_list = {};

if draw_heat_map
    % for subplots
    figure;
end

for i = 1:length(routerNameList)
    routerName = routerNameList{i};
    routerLabel = routerLabelList{i};
    
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
            'FPMatrix', 'TPMatrix', 'FNMatrix', 'round',...
            'counterToIndexMap', 'rateToIndexMap');
    end
    
    total_damageMatrix_list{i} = total_damageMatrix;
    FNMatrix_list{i} = FNMatrix;
    
    fig_dir = ['./exp_logger/', expName, '/fig/'];
    mkdir(fig_dir);
    
    if draw_heat_map
        lineWidth = 0;
        % get mesh grid
        yt = min(rateList) * 4.^(0:5);
        ytl = {};
        for k = 1:length(yt)
            num10 = floor(log10(yt(k)));
            ytl{k} = [num2str(yt(k)/10^num10, 2), 'x10^', num2str(num10)]; 
        end
        
        [X, Y] = meshgrid(counterList, log10(rateList));

        n = 40;
        cmap = hot;
        default_constant_color = 'k';

        if draw_total_damage
        max_damage = max_damages(exp_idx);
        for x = 1:size(total_damageMatrix, 1)
            for y = 1:size(total_damageMatrix, 2)
                if total_damageMatrix(x, y) > max_damage;
                    total_damageMatrix(x, y) = max_damage;
                end
            end
        end
            
%         fig = figure;
        subplot(1,length(routerNameList),i);
        contourf(X, Y, total_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
%         set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        h = colorbar();
%         title([routerLabel, ' Damage, \theta=', num2str(thetas(exp_idx))]);
        caxis([0, max_damage]);
        title([routerLabel, ' Damage']);
        xlabel('Counter Number');
        ylabel('Large Flow Rate (B/s)');
%         saveas(fig, [fig_dir, routerName, '-total-damage.eps'], 'epsc');
        end

        if draw_be_damage
        fig = figure;
        contourf(X, Y, be_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
        set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        colorbar();
        title([routerLabel, ' BE Damage, \theta=', num2str(thetas(exp_idx))]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-be-damage.eps'], 'epsc');
        end
        
        if draw_fp_damage
        fig = figure;
        contourf(X, Y, fp_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
        set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        colorbar();
        title([routerLabel, ' FP Damage, \theta=', num2str(thetas(exp_idx))]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-fp-damage.eps'], 'epsc');
        end
        
        if draw_qd_damage
        fig = figure;
        contourf(X, Y, qd_damageMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
        set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        colorbar();
        title([routerLabel, ' Drop Damage, \theta=', num2str(thetas(exp_idx))]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-qd-damage.eps'], 'epsc');
        end
        
        if draw_fp
        fig = figure;
        contourf(X, Y, FPMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap);  
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
        set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        colorbar();
        title([routerLabel, ' FP, \theta=', num2str(thetas(exp_idx))]);
        %         if drawRatio
        %             title(['Ratio of FP of ', detectorName]);
        %         end
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-FP.eps'], 'epsc');
        end

        if draw_tp
        % TODO: Fix bug to draw the figure with value of TP
        fig = figure;
        contourf(X, Y, TPMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap); 
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
        set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        colorbar();
        title([routerLabel, ' TP, \theta=', num2str(thetas(exp_idx))]);
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-TP.eps'], 'epsc');
        end

        if draw_fn
        fig = figure;
        contourf(X, Y, FNMatrix, n, 'LineWidth',lineWidth);
        colormap(cmap); 
        set(gca,'YTick',log10(yt),'YTickLabel',ytl);
        set(gca,'color',default_constant_color);
        set(gcf,'Position',[100 100 265 160]);
        ylim([log10(min(yt)), log10(max(yt))]);
        colorbar();
        title([routerLabel, ' FN, \theta=', num2str(thetas(exp_idx))]);
        %         if drawRatio
        %             title(['Ratio of FN of ', detectorName]);
        %         end
        xlabel('Counter Number');
        ylabel('Large Flow Rate');
        saveas(fig, [fig_dir, routerName, '-FN.eps'], 'epsc');
        end
    end
end

% end of subplot
if draw_heat_map
    set(gcf,'Position',[100 100 1080 140]);
end


all_total_damageMatrix_list{exp_idx} = total_damageMatrix_list;
all_FNMatrix_list{exp_idx} = FNMatrix_list;

% draw damage comparison figure at a # of counter
% fig_num_counters = [40, 100, 200, 400];
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
%     title(['Damage, m=', num2str(fig_num_counter), ', \theta=', num2str(thetas(exp_idx))]);
    title(['Damage, m=', num2str(fig_num_counter)]);
    xlabel('Large Flow Rate (B/s)');
    ylabel('Damage (Byte)');
%     ylim([10^6, 5000000000]);
    ylim([0, damage_ylim(exp_idx)]);
    legend(routerLabelList);
    set(gcf,'Position',[100 100 250 150]);
    saveas(fig, [fig_dir, 'total_damage_at_counter_', num2str(fig_num_counter), '.pdf'], 'pdf');
end

maxFN = 10;
for j = 1:length(fig_num_counters)
    fig_num_counter = fig_num_counters(j);
    counterIndex = counterToIndexMap(fig_num_counter);
    fig = figure;
    for i = 1:length(routerNameList)
        FNMatrix = FNMatrix_list{i};
        if thetas(exp_idx) < 1.0
            semilogx(rateList, FNMatrix(:, counterIndex)/maxFN, colors{i});
        else
            tmp = FNMatrix(:, counterIndex);
            semilogx(rateList, [10; FNMatrix(2:length(tmp), counterIndex)]/maxFN, colors{i});
        end
        hold on;
    end
%     title(['FN, m=', num2str(fig_num_counter), ', \theta=', num2str(thetas(exp_idx))]);
    title(['FN Ratio, m=', num2str(fig_num_counter)]);
    xlabel('Large Flow Rate (B/s)')
    ylabel('FN Ratio');
    legend(routerLabelList);
    set(gcf,'Position',[100 100 250 150]);
    saveas(fig, [fig_dir, 'FN_at_counter_', num2str(fig_num_counter), '.pdf'], 'pdf');
end

end


% draw figures across for efd-eardet
ee_fig_dir = ['./exp_logger/fig/'];
mkdir(ee_fig_dir);
% fig_num_counters = [40, 100, 200, 400];
colors = {'b-*', 'r-x', 'm-v', 'k-^', 'g-d', 'c-o', 'y-+'};
for j = 1:length(fig_num_counters)
    fig_num_counter = fig_num_counters(j);
    counterIndex = counterToIndexMap(fig_num_counter);
    fig = figure;
    legend_str = {};
    for i = 1:length(expNames)
        total_damageMatrix = all_total_damageMatrix_list{i}{efd_eardet_index};
        semilogx(rateList, total_damageMatrix(:, counterIndex), colors{i});
        hold on;
        legend_str{i} = ['\theta = ', num2str(thetas(i))];
    end
    title(['Damage, m=', num2str(fig_num_counter)]);
    xlabel('Large Flow Rate (B/s)');
    ylabel('Damage (Byte)');
    legend(legend_str)
%     ylim([10^6, 5000000000]);
    ylim([0, 1.2*10^8]);
    set(gcf,'Position',[100 100 250 150]);
    saveas(fig, [ee_fig_dir, 'ee_total_damage_at_counter_', num2str(fig_num_counter), '.pdf'], 'pdf');
    
    fig = figure;
    maxFN = 10;
    for i = 1:length(expNames)
        FNMatrix = all_FNMatrix_list{i}{efd_eardet_index};
        if thetas(i) < 1.0
            semilogx(rateList, FNMatrix(:, counterIndex)/maxFN, colors{i});
        else
            tmp = FNMatrix(:, counterIndex);
            semilogx(rateList, [10; FNMatrix(2:length(tmp), counterIndex)]/maxFN, colors{i});
        end
        hold on;
        legend_str{i} = ['\theta = ', num2str(thetas(i))];
    end
    title(['FN Ratio, m=', num2str(fig_num_counter)]);
    xlabel('Large Flow Rate (B/s)');
    ylabel('FN Ratio');
    legend(legend_str)
    set(gcf,'Position',[100 100 250 150]);
    saveas(fig, [ee_fig_dir, 'ee_FN_at_counter_', num2str(fig_num_counter), '.pdf'], 'pdf');
end



clear all, close all, clc;


rl_data = load('./mini_scale_2/rl_result_ms.txt');
bf_data = load('./mini_scale_2/bf_damage_ms_bf_1.txt');
% bf_data_2 = load('./mini_scale_2/bf_damage_ms_bf_2.txt');
rl_onfly_data = load('./mini_scale_2/rl_onfly_result_ms.txt');
TH = 0.333333333;
TH = 0.41;

% rl_data = load('./toy_scale_2/rl_result_ts.txt');
% bf_data = load('./toy_scale_2/bf_damage_ts_bf_100r.txt');
% rl_onfly_data = load('./toy_scale_2/rl_onfly_result_ts.txt');
% TH = 0.1666667;
% TH = 0.3

rl_rate = rl_data(:, 1);
rl_oversent = rl_data(:, 2);
rl_life_time = rl_data(:, 3);
rl_damage = rl_oversent - TH .* rl_life_time;

bf_rate = bf_data(:,1);
bf_damage = bf_data(:,2);
bf_life_time = bf_data(:,3);
bf_damage_calculated = bf_life_time .* (bf_rate - TH);
% bf_rate_2 = bf_data_2(:,1);
% bf_damage_2 = bf_data_2(:,2);
% bf_life_time_2 = bf_data_2(:,3);
% bf_damage_calculated_2 = bf_life_time_2 .* (bf_rate_2 - TH);

% by rounds
rl_onfly_rounds = [];
rl_onfly_rates = {};
rl_onfly_oversent = {};
rl_onfly_life_time = {};
rl_onfly_damage = {};
last_round = -1;
count = 0;
for i = 1:size(rl_onfly_data, 1)
    round = rl_onfly_data(i,1);
    if round ~= last_round
        count = count  + 1;
        last_round = round;
        rl_onfly_rounds(count) = round;
        rl_onfly_rates{count} = [];
        rl_onfly_oversent{count} = [];
        rl_onfly_life_time{count} = [];
        rl_onfly_damage{count} = [];
    end
    rl_onfly_rates{count} = [rl_onfly_rates{count}, rl_onfly_data(i, 2)];
    rl_onfly_oversent{count} = [rl_onfly_oversent{count}, rl_onfly_data(i, 3)];
    rl_onfly_life_time{count} = [rl_onfly_life_time{count}, rl_onfly_data(i, 4)];
    tmp_damage = rl_onfly_data(i, 3) - TH * rl_onfly_data(i, 4);
    rl_onfly_damage{count} = [rl_onfly_damage{count}, tmp_damage];
end
rl_onfly_max_damage = [];
for i = 1:length(rl_onfly_damage)
    max_damage = max(rl_onfly_damage{i});
    rl_onfly_max_damage = [rl_onfly_max_damage, max_damage];
end



figure;
plot(rl_rate, rl_damage, '*-b');
hold on;
% plot(bf_rate, bf_damage, '*-m');
plot(bf_rate, bf_damage_calculated, '*-m');
% plot(bf_rate_2, bf_damage_calculated_2, '*-g');
plot([TH, TH + 0.0001], [-1000, 100], '-r')
title('max damage');
xlabel('rate');
ylabel('damage');
% legend({'by RL', 'by BF 1', 'by BF 2', 'Threshold'});
legend({'by RL', 'by BF', 'Threshold'});
ylim([-20, 100]);

figure;
plot(rl_rate, rl_life_time, '*-b');
hold on
plot(bf_rate, bf_life_time, '*-m');
% plot(bf_rate_2, bf_life_time_2, '*-g');
plot([TH, TH + 0.0001], [-1000, 1000], '-r')
title('max life time');
xlabel('rate');
ylabel('damage');
% legend({'by RL', 'by BF 1', 'by BF 2', 'Threshold'});
legend({'by RL', 'by BF', 'Threshold'});
ylim([0, 1000]);

% by rounds
color = {'b', 'r', 'k', 'g', 'y', 'c'};
figure;
plot(bf_rate, bf_damage_calculated, '*-m');
hold on;
N = 6;
count = 0;
mylegend = {'by BF'};
for i = 1:ceil(length(rl_onfly_rounds) / N):length(rl_onfly_rounds)
    count = count + 1;
    round = rl_onfly_rounds(i);
    rates = rl_onfly_rates{i};
%     oversent = rl_onfly_oversent{i};
%     life_time = rl_onfly_life_time{i};
    damage = rl_onfly_damage{i};
    plot(rates, damage, ['*-', color{count}]);
    mylegend{1 + count} = ['By RL, round ', num2str(round)];
end
plot([TH, TH + 0.0001], [-1000, 100], '--r')
mylegend{2 + count} = 'Threshold';
title('max damage');
xlabel('rate');
ylabel('damage');
% legend({'by RL', 'by BF 1', 'by BF 2', 'Threshold'});
legend(mylegend);
ylim([-20, 100]);

figure;
plot(rl_onfly_rounds, rl_onfly_max_damage, '*-b');
hold on;
max_bf_damage = max(bf_damage_calculated);
plot([1, max(rl_onfly_rounds)], [1, 1] * max_bf_damage, '*-m');
title('Max Damage RL v.s. BF');
xlabel('rounds');
ylabel('max damage');
legend({'By RL', 'By BF'});
ylim([-20, 100]);



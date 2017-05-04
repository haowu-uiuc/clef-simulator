clear all, close all, clc;

rl_data = load('./mini_scale/rl_result_ms.txt');
bf_data = load('./mini_scale/bf_damage_ms_bf_1.txt');
bf_data_2 = load('./mini_scale/bf_damage_ms_bf_2.txt');

TH = 0.333333333;
% TH = 0.41;
rl_rate = rl_data(:, 1);
rl_oversent = rl_data(:, 2);
rl_life_time = rl_data(:, 3);
rl_damage = rl_oversent - TH .* rl_life_time;

bf_rate = bf_data(:,1);
bf_damage = bf_data(:,2);
bf_life_time = bf_data(:,3);
bf_damage_calculated = bf_life_time .* (bf_rate - TH);
bf_rate_2 = bf_data_2(:,1);
bf_damage_2 = bf_data_2(:,2);
bf_life_time_2 = bf_data_2(:,3);
bf_damage_calculated_2 = bf_life_time_2 .* (bf_rate_2 - TH);

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

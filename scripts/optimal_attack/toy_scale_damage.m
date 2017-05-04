clear all, close all, clc;

rl_data = load('./toy_scale/rl_result_ts.txt');
bf_data = load('./toy_scale/bf_damage_ts_bf_100r.txt');

TH = 0.1666666667;
TH = 0.3;
rl_rate = rl_data(:, 1);
rl_oversent = rl_data(:, 2);
rl_life_time = rl_data(:, 3);
rl_damage = rl_oversent - TH .* rl_life_time;

bf_rate = bf_data(:,1);
bf_damage = bf_data(:,2);
bf_life_time = bf_data(:,3);
bf_damage_calculated = bf_life_time .* (bf_rate - TH);

figure;
% plot(bf_rate, bf_damage, '*-m');
plot(bf_rate, bf_damage_calculated, '*-m');
hold on;
plot(rl_rate, rl_damage, '*-b');
% plot(bf_rate_2, bf_damage_calculated_2, '*-g');
plot([TH, TH + 0.0001], [-1000, 100], '-r')
title('max damage');
xlabel('rate');
ylabel('damage');
% legend({'by RL', 'by BF 1', 'by BF 2', 'Threshold'});
legend({'by RL', 'by BF', 'Threshold'});
ylim([-20, 100]);

figure;
plot(bf_rate, bf_life_time, '*-m');
hold on
plot(rl_rate, rl_life_time, '*-b');
% plot(bf_rate_2, bf_life_time_2, '*-g');
plot([TH, TH + 0.0001], [-1000, 1000], '-r')
title('max life time');
xlabel('rate');
ylabel('damage');
% legend({'by RL', 'by BF 1', 'by BF 2', 'Threshold'});
legend({'by RL', 'by BF', 'Threshold'});
ylim([0, 1000]);

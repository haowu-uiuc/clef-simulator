clear all, close all, clc;

prob = [0.06, 0.125, 0.175, 0.2, 0.25, 0.3, 0.4, 0.5, 1.0];
oversent = [15.7, 20.8 , 29.1, 20, 42.6, 56, 26, 94, 154];
life_time = [15.7, 25.8, 56.6, 22, 134.5, 174, 58, 319, 1000];
rate = [1.0, 0.77, 0.50, 0.89, 0.31, 0.3, 0.52, 0.28, 0.154];
TH = 0.154;

% toy scale:
rl_data = load('./toy_scale/rl_result_ts.txt');
TH = 0.166667;
% rl_data = load('./mini_scale/rl_result_ms.txt');
% TH = 0.333333333;
rate = rl_data(:, 1);
oversent = rl_data(:, 2);
life_time = rl_data(:, 3);

damage = oversent - TH .* life_time;

figure;
plot(rate, damage, '*-');
hold on;
plot([TH, TH + 0.0001], [-1000, 100], '-r')
title('max damage');
xlabel('rate');
ylabel('damage');
ylim([-20, 100]);

figure;
plot(rate, life_time, '*-');
hold on;
plot([TH, TH + 0.0001], [0, 1000], '-r')
title('life time');
xlabel('rate');
ylabel('life time');



% draw brute froce result
data = load('./mini_scale/bf_damage_ms_bf_2.txt');
% data = load('./result_debug_exp.txt');
threshold = 0.333;

figure;
plot(data(:,1), data(:,2), '*-');
hold on;
plot([threshold, threshold + 0.0001], [-1000, 100], '-r')
title('max damage');
xlabel('rate');
ylabel('damage');
ylim([-20, 100]);

figure;
plot(data(:,1), data(:,3), '*-');
hold on;
plot([threshold, threshold + 0.0001], [0, 1000], '-r')
title('life time');
xlabel('rate');
ylabel('life time');


% rl_dir = './toy_scale';
% rate = [];
% volume = [];
% lifetime = [];
% 
% for i = 1:100
%     file_path = [rl_dir, '/rl_result_ts_prob_', num2str(i),'.txt'];
%     if exist(file_path, 'file') == 2 
%         % read file
%         fid = fopen(file_path, 'r');
%         fgets(fid);
%         line = fgets(fid);
%         display(line);
%     end
% end



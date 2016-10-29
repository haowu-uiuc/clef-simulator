clear all, close all, clc;

num_balls_list = [100000, 1000];
num_bins_list = [100, 100];
rounds_list = [1000000 * ones(1, 2)];
% 
% num_balls_list = [100000, 100];
% num_bins_list = [1000, 1000];
% rounds_list = [10000, 1000000 * ones(1, 1)];

n_gamma = 100000;
% max_top = 0;

total_flow_size = 1:2000;
% total_flow_size = 1:70;

total_cdf_prob = ones(1, length(total_flow_size));
first_levels_prob = ones(1, length(total_flow_size));

for j = 1:length(num_balls_list)
    num_balls = num_balls_list(j);
    num_bins = num_bins_list(j);
    rounds = rounds_list(j);
    
    result_file_name = ['prob-diffs-', num2str(num_bins), '-', num2str(num_balls), '.txt'];
    result_file_path = ['./data/', num2str(rounds),'-runs/', result_file_name];

    data = load(result_file_path);

    cdf_prob_list = {};
    cdf_flow_size_list = {};
    last_max_top = -1;
    for i = 1:size(data, 1)        
        max_top = data(i, 1);
        if (last_max_top < max_top)
            if (last_max_top >= 0)
                cdf_flow_size_list{last_max_top+1} = cdf_flow_size;
                cdf_prob_list{last_max_top+1} = cdf_prob;
            end

            last_max_top = max_top;
            cdf = 0;
            cdf_prob = [0];
            cdf_flow_size = [1];
            idx = 1;
        end

        diff = data(i, 2);
        prob = data(i, 3);
        
        if (diff <= 0)
            cdf = cdf + prob;
            cdf_prob(1) = cdf;
        else
            idx = idx + 1;
            cdf = cdf + prob;
            cdf_prob(idx) = cdf;
            cdf_flow_size(idx) = diff + 1;
        end
    end
    
    cdf_flow_size_list{max_top+1} = cdf_flow_size;
    cdf_prob_list{max_top+1} = cdf_prob;

    prob_interp = interp1(cdf_flow_size_list{1},cdf_prob_list{1},total_flow_size);
    for i = 1:length(total_flow_size)
        if isnan(prob_interp(i))
            prob_interp(i) = 1.0;
        end
    end
    
    total_cdf_prob = total_cdf_prob .* prob_interp;    
    if num_balls >= n_gamma
       first_levels_prob = total_cdf_prob; 
    end
end

% load actual P_worst
result_file_path = ['./data/total-prob-1000-runs/total-prob-100-100000-2.txt'];
actual_data = load(result_file_path);
actual_prob = actual_data(:,2);
actual_prob_flow_size = actual_data(:,1);

% draw figure
fig = figure;
legend_list = {'Actual Pr_{worst}', 'Approx. Pr_{worst}'};
% plot(total_flow_size, total_cdf_prob, '-*b');
plot(actual_prob_flow_size, actual_prob, '-*b');
hold on;
plot(total_flow_size, first_levels_prob, '-m', 'LineWidth',2);

% calculate theoretical bound
n = num_balls_list(1);
m = num_bins(1);
lt = n/m;
a = total_flow_size;
ymax = n/m + sqrt(2*n/m*log(n));
k = ymax - a;
d_gamma = floor(log(n/n_gamma)/log(m)) + 1;
p = (1 - poisscdf(k, lt)).^d_gamma;
legend_list{length(legend_list)+1} = 'Pr_{worst} lower bound';
plot(a, p, 'r--');

title(['Total Probability. n=', num2str(n), ' m=', num2str(num_bins)]);
xlabel('\alpha = R_{atk}/\gamma');
ylabel('Probability');
ylim([0, 1.3]);
xlim([1, 250]);
legend(legend_list);
set(gcf,'Position',[100 100 400 200]);


% damage trend
figure;
d_bound = (a-1) ./ p;
t = min(ones(1, length(p)) * 100, 1./total_cdf_prob);
d = (total_flow_size-1) ./ total_cdf_prob;
% semilogy(total_flow_size, d, '*-b');
plot(total_flow_size, d, '*-b');
hold on
plot(a, d_bound, '--r');
% EARDet limit
plot([n/(m+1), n/(m+1)+0.1], [10^10, 1], 'k--')
legend('By P_{worst}', 'Upperbound', 'EARDet Limit \gamma_{h}');
ylim([100, 2*10^4]);
xlim([0, 1200]);
title('\theta = 1.0');
xlabel('\alpha = R_{atk}/\gamma');
ylabel('Theoretical Damage');
set(gcf,'Position',[100 100 250 200]);


% bursty flow damage trend
thetas = [0.8, 0.5, 0.25, 0.15];
for i = 1:length(thetas)
theta = thetas(i);
figure;
d_bound = (a-1) ./ p / theta;
t = min(ones(1, length(p)) * 100, 1./total_cdf_prob);
d = (total_flow_size-1) ./ total_cdf_prob / theta;
% semilogy(total_flow_size, d, '*-b');
plot(total_flow_size / theta, d, '*-b');
hold on
plot(a / theta, d_bound, '--r');
% EARDet limit
plot([n/(m+1) * theta, n/(m+1) * theta+0.1], [10^10, 1], 'k--')
legend('By P_{worst}', 'Upperbound', 'EARDet Limit \gamma_{h}');
ylim([100, 2*10^4]);
xlim([0, 2000]);
title(['\theta=', num2str(theta), ', \theta T_b >= 2T_c^{(1)}']);
xlabel('\alpha = R_{atk}/\gamma');
ylabel('Theoretical Damage');
set(gcf,'Position',[100 100 250 200]);
end


% bursty flow damage trend for twin-efd
thetas = [0.8, 0.5, 0.25, 0.15];
a1 = 2*(2*n/m*log(n))^0.5;
for i = 1:length(thetas)
theta = thetas(i);
figure;
gamma_h = n/(m+1);
depth = floor(log(n)/log(m)) + 1;
d_bound = (a-1) ./ p * depth * gamma_h * 2 / a1;
t = min(ones(1, length(p)) * 100, 1./total_cdf_prob);
d = (total_flow_size-1) ./ total_cdf_prob * depth * gamma_h * 2 / a1;
% semilogy(total_flow_size, d, '*-b');
plot(total_flow_size, d, '*-b');
hold on
plot(a, d_bound, '--r');
% EARDet limit
plot([n/(m+1) * theta, n/(m+1) * theta+0.1], [10^10, 1], 'k--')
legend('By P_{worst}', 'Upperbound', 'EARDet Limit \gamma_{h}');
ylim([100, 5*10^4]);
xlim([0, 2000]);
title(['\theta=', num2str(theta), ', \theta T_b < 2T_c^{(1)}']);
xlabel('\alpha = R_{atk}/\gamma');
ylabel('Theoretical Damage');
set(gcf,'Position',[100 100 250 200]);
end





% calculate experiment setting:
n = 10000;
m = [5, 10, 17, 25, 37, 50, 100];
d = floor(log(n)./log(m) * 1.2)+1
a05 = (2.*n./m.*log(n)).^0.5;
gamma_h = n./(2.*m+1);
T2 = 2.*d.*gamma_h./a05./1.*1 * 1.5
T2_d = T2./d
clear all, close all, clc;

num_balls_list = [50, 100, 1000, 10000, 100000];
num_balls_list = [num_balls_list, num_balls_list];
num_bins_list = [100, 100, 100, 100, 100];
num_bins_list = [num_bins_list, num_bins_list*10];
rounds_list = [1000000 * ones(1, 10)];
% max_top = 0;


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


% draw
% max_tops = [1, 2, 5, 10];
max_tops = [1];
draw_cdf(max_tops, cdf_prob_list, cdf_flow_size_list, num_balls, num_bins);
end


%%%%%% figure show the worst-case background traffic %%%%%
fig = figure;
colors = {'b-*', 'k-^', 'r-x', 'm-v', 'g-d', 'c-o', 'y-+'};
rho = 1000;
num_balls_list = [100000, 10000, 1000];
num_bins_list = [100, 100, 100];
gammas = [0.01, 0.1, 1]
rounds_list = [1000000 * ones(1, 3)];
max_flow_size = 0;
legend_list = {};
for j = 1:length(num_balls_list)
    num_balls = num_balls_list(j);
    num_bins = num_bins_list(j);
    rounds = rounds_list(j);
    gamma = gammas(j)
    
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

    max_alpha = 1000;
    
    max_tops = [1];
    for i = 1:length(max_tops)
        max_fs = floor(max(cdf_flow_size_list{max_tops(i)})* gamma);
        max_prob_array = max_fs+1:0.5:max_alpha;
        
        plot([cdf_flow_size_list{max_tops(i)} * gamma, max_prob_array], [cdf_prob_list{max_tops(i)}, ones(1, length(max_prob_array))], colors{j});
        hold on;
        if (gamma ~= 1)
            legend_list{j} = [num2str(rho/gamma), ' flows; background rate = ', num2str(gamma)];
        else
            legend_list{j} = [num2str(rho/gamma), ' flows; background rate = \gamma'];
        end
        max_flow_size = max(max_flow_size, max(cdf_flow_size_list{max_tops(i)}) * gamma);
    end   
end

title('Detection Probability in a Single Level');
xlabel('\alpha = R_{atk}/\gamma')
ylabel('Probability');
ylim([0, 1.5]);
xlim([1, max_flow_size * 0.8]);
legend(legend_list);
set(gcf,'Position',[100 100 300 150]);

saveas(fig, ['./fig/worst_background.pdf'], 'pdf');






% draw theoretical bound (normal distribution approximation)
% not accurate at all!
% a = 1:100;
% n = num_balls;
% m = num_bins;
% z = (sqrt(2*n/m*log(n))-a)/(n/m);
% figure;
% plot(a, 1 - normcdf(z), '*-');
% 
% 
% % poisson approximation:
% lt = n/m;
% a = 1:100;
% ymax = n/m + sqrt(2*n/m*log(n));
% k = ymax - a;
% % p = exp(-lt) * (lt).^k ./ factorial(k);
% p = 1 - poisscdf(k, lt);
% figure;
% plot(a, p);
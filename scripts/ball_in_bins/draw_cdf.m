function draw_cdf(max_tops, cdf_prob_list, cdf_flow_size_list, num_balls, num_bins)
    colors = {'b-*', 'r-x', 'm-v', 'k-^', 'g-d', 'c-o', 'y-+'};
    
    max_alpha = 1000;
    
    fig = figure;
    legend_list = {};
    max_flow_size = 0;
    for i = 1:length(max_tops)
        plot([cdf_flow_size_list{max_tops(i)}, max_alpha], [cdf_prob_list{max_tops(i)}, 1], colors{i});
        hold on;
        if (length(max_tops) == 1 && max_tops(i) == 1)
            legend_list{i} = 'Simulated prob.';
        else
            legend_list{i} = [num2str(max_tops(i)), '-branches sim. prob.'];
        end
        max_flow_size = max(max_flow_size, max(cdf_flow_size_list{max_tops(i)}));
    end
    
    % calculate theoretical bound
    n = num_balls;
    m = num_bins;
    lt = n/m;
    a = 1:max_alpha;
    ymax = n/m + sqrt(2*n/m*log(n));
    k = ymax - a;
    p = 1 - poisscdf(k, lt);
    legend_list{length(legend_list)+1} = 'Approx. lower bound';
    plot(a, p, 'r--');
    
    title(['n=', num2str(num_balls), ' m=', num2str(num_bins)]);
    xlabel('\alpha = R_{atk}/\gamma')
    ylabel('Probability');
    ylim([0, 1.3]);
    xlim([1, max_flow_size * 1.2]);
    legend(legend_list);
    set(gcf,'Position',[100 100 200 160]);
    
    saveas(fig, ['./fig/det_prob_', num2str(num_bins), '_c_', num2str(num_balls),...
        '_f', '.pdf'], 'pdf');
    
    
end


clear all, close all, clc;

rou = 25000000;
alpha = 1514;
beta = 6056;
R = 250000;
r = 25000;
t = 1;

Flows = load('./realtrace_long.txt');


counter = 0;
T = [];
C = [];
t = 0;
t_last = 0;
for i = 1:length(Flows)
   if(Flows(i,1) == 2416)
        t = Flows(i,3);

        counter = counter - (t- t_last)*r;
        if(counter < 0)
            counter = 0;
        end
        
        counter = counter + Flows(i,2)*(rou-r)/rou;
        
        T = [T, t];
        C = [C, counter];
        t_last = t;
   end
end

figure;
plot(T, C, 'b-*');
hold on;
plot((1:30), ones(1,30)*beta, 'k-.');

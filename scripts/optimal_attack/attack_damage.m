clear all, close all, clc;

prob = [0.06, 0.125, 0.175, 0.2, 0.25, 0.3, 0.4, 0.5, 1.0];
oversent = [15.7, 20.8 , 29.1, 20, 42.6, 56, 26, 94, 154];
life_time = [15.7, 25.8, 56.6, 22, 134.5, 174, 58, 319, 1000];
rate = [1.0, 0.77, 0.50, 0.89, 0.31, 0.3, 0.52, 0.28, 0.154];

TH = 0.154;
damage = oversent - TH .* life_time;

figure;
plot(rate, damage, '*');
from clef_env import ClefEnv

import numpy as np
import tensorflow as tf
import os
import sys
import json


class QNet:
    def __init__(self, input_size, num_actions, config=None):
        self.exp_name = "test_exp"
        self.input_size = input_size
        self.num_actions = num_actions
        self.learning_rate = 0.001
        self.gamma = 0.99
        self.e = 0.1

        if config is not None:
            if "INPUT_SIZE" in config:
                self.input_size = config["INPUT_SIZE"]
            if "EXP_NAME" in config:
                self.exp_name = config["EXP_NAME"]
            if "LEARNING_RATE" in config:
                self.learning_rate = config["LEARNING_RATE"]
            if "DISCOUNT_FACTOR" in config:
                self.gamma = config["DISCOUNT_FACTOR"]

        tf.reset_default_graph()
        # These lines establish the feed-forward part of
        # the network used to choose actions
        N = self.input_size
        H = 2 * self.input_size
        M = num_actions
        self.inputs = tf.placeholder(shape=[None, N], dtype=tf.float32)
        self.W1 = tf.Variable(
            tf.truncated_normal([N, H], stddev=0.1), name='W1')
        self.b1 = tf.Variable(
            tf.truncated_normal([H], stddev=0.1), name='b1')
        z1 = tf.matmul(self.inputs, self.W1) + self.b1
        y1 = tf.nn.relu(z1)  # TODO: choose the right non-linear func

        self.W2 = tf.Variable(
            tf.truncated_normal([H, H], stddev=0.1), name='W2')
        self.b2 = tf.Variable(
            tf.truncated_normal([H], stddev=0.1), name='b2')
        z2 = tf.matmul(y1, self.W2) + self.b2
        y2 = tf.nn.relu(z2)  # TODO: choose the right non-linear func

        self.W3 = tf.Variable(
            tf.truncated_normal([H, H], stddev=0.1), name='W3')
        self.b3 = tf.Variable(
            tf.truncated_normal([H], stddev=0.1), name='b3')
        z3 = tf.matmul(y2, self.W3) + self.b3
        y3 = tf.nn.relu(z3)  # TODO: choose the right non-linear func

        self.W4 = tf.Variable(
            tf.truncated_normal([H, 1], stddev=0.1), name='W4')
        self.b4 = tf.Variable(
            tf.truncated_normal([1], stddev=0.1), name='b4')
        z4 = tf.matmul(y3, self.W4) + self.b4
        self.prob = tf.sigmoid(z4)

        self.input_y = tf.placeholder(tf.float32, [None, 1], name="input_y")
        self.advantages = tf.placeholder(tf.float32, name="reward_signal")
        loglik = tf.log(
            self.input_y * (self.input_y - self.prob) +
            (1 - self.input_y) * (self.input_y + self.prob))
        self.loss = -tf.reduce_mean(loglik * self.advantages)

        # self.trainer = tf.train.GradientDescentOptimizer(learning_rate=0.01)
        self.trainer = tf.train.AdamOptimizer(
            learning_rate=self.learning_rate, epsilon=1e-8)
        self.updateModel = self.trainer.minimize(self.loss)

        init = tf.initialize_all_variables()
        self.sess = tf.Session()
        self.sess.run(init)
        self.saver = tf.train.Saver()

    def _discount_rewards(self, r):
        """ take 1D float array of rewards and compute discounted reward """
        discounted_r = np.zeros_like(r)
        running_add = 0
        for t in reversed(xrange(0, r.size)):
            running_add = running_add * self.gamma + r[t]
            discounted_r[t] = running_add
        # size the rewards to be unit normal
        # (helps control the gradient estimator variance)
        discounted_r -= np.mean(discounted_r)
        discounted_r /= np.std(discounted_r)
        return discounted_r

    def _discount_rewards_sw(self, r, ws):
        """ take 1D float array of rewards and compute discounted reward
        only consider the estimated reword for the next ws-1 slots"""
        discounted_r = np.zeros_like(r)
        ws_discount = np.power(self.gamma, ws)

        running_count = 0
        running_add = 0
        for t in reversed(xrange(0, r.size)):
            running_add = running_add * self.gamma + r[t]
            running_count += 1
            if running_count == ws + 1:
                # remove the head of the queue out of the window
                # with the discount added on it
                running_add -= r[t + ws] * ws_discount
                running_count -= 1
            discounted_r[t] = running_add

        # for t in reversed(xrange(0, r.size)):
        #     std = 0
        #     mean = 0
        #     if t - ws + 1 >= 0:
        #         std = np.std(discounted_r[t - ws + 1:t + 1])
        #         mean = np.mean(discounted_r[t - ws + 1:t + 1])
        #     else:
        #         std = np.std(discounted_r[:t + 1])
        #         mean = np.mean(discounted_r[:t + 1])
        #     discounted_r[t] -= mean
        #     if std > 0:
        #         discounted_r[t] /= std

        return discounted_r

    def _generate_input(self, t, status):
        if t == 0:
            return [[0.5] * self.input_size]
        if t > 0 and t < self.input_size:
            input_val = [0.5] * self.input_size
            input_val[self.input_size - t:] = status[:t]
        else:
            input_val = status[t - self.input_size:t]
        return [input_val]

    def train(self, env, num_episodes=2000):
        # create lists to contain total rewards and steps per episode
        rsum_100 = 0
        oversent_100 = 0
        tsum_100 = 0
        ave_rate_100 = 0
        batch_to_print = 100
        batch_to_print_detail = 1000

        for i in range(num_episodes):
            xs, drs, ys = [], [], []

            if i % batch_to_print == 0:
                print "episode " + str(i)
            # Reset environment and get first new observation
            s = env.reset()
            rAll = 0
            oversent = 0
            done = False
            t = -1
            # The Q-Table learning algorithm
            while True:
                # print t
                t += 1
                # Choose an action by greedily (with e chance of random action)
                # from the Q-network
                # input_val = [s]
                input_val = self._generate_input(t, s)
                x = np.reshape(input_val, [1, self.input_size])
                tfprob = self.sess.run(
                    self.prob, feed_dict={self.inputs: x})

                action = 1 if np.random.uniform() < tfprob else 0

                # # still keep looking at another chance
                # if np.random.uniform() < self.e:
                #     action = 1 - action

                xs.append(x)    # observation
                y = 1 if action == 0 else 0     # a "fake label"
                ys.append(y)

                # Get new state and reward from environment
                s, r, done, info = env.step(action)
                drs.append(float(r))

                rAll += r
                rsum_100 += r
                oversent += action
                oversent_100 += action

                if i % batch_to_print_detail == 0:
                    print """{ts}. [{action}], r = {reward}, \
                            prob_to_sent = {prob}""".format(
                        ts=t,
                        reward=r,
                        action=action,
                        prob=tfprob)
                    if info[1]:
                        print "=============="
                    elif info[0]:
                        print "--------------"

                if done:
                    tsum_100 += t
                    ave_rate_100 += float(oversent) / (t + 1)

                    epx = np.vstack(xs)
                    epy = np.vstack(ys)
                    epr = np.vstack(drs)

                    # compute the discounted reward backwards through time
                    # discounted_epr = self._discount_rewards(epr)
                    discounted_epr = self._discount_rewards_sw(
                        epr, self.input_size)

                    # if i % batch_to_print == 0:
                    #     print discounted_epr
                    self.sess.run(self.updateModel, feed_dict={
                        self.inputs: epx, self.input_y: epy,
                        self.advantages: discounted_epr})
                    break

            if i % batch_to_print == 0:
                print "----Episode %d----" % i
                print "Average damage in an episode:"\
                    + str(oversent_100 / batch_to_print)
                print "Average life time in an episode:"\
                    + str(tsum_100 / batch_to_print)
                print "Average rate in life time in an episode:"\
                    + str(ave_rate_100 / batch_to_print)
                rsum_100 = 0
                oversent_100 = 0
                tsum_100 = 0
                ave_rate_100 = 0

        # save the model
        model_dir = './' + self.exp_name + '_model'
        if not os.path.exists(model_dir):
            os.makedirs(model_dir)
        self.saver.save(self.sess, model_dir + '/' + self.exp_name)

        print "Final Model:"
        print "W1 = " + str(self.sess.run(self.W1))
        print "b1 = " + str(self.sess.run(self.b1))
        print "W2 = " + str(self.sess.run(self.W2))
        print "b2 = " + str(self.sess.run(self.b2))
        print "W3 = " + str(self.sess.run(self.W3))
        print "b3 = " + str(self.sess.run(self.b3))
        print "W4 = " + str(self.sess.run(self.W4))
        print "b4 = " + str(self.sess.run(self.b4))

    # TODO refactor the test function
    # def test(self, env):
    #     # run the game with trained Q table:
    #     # new_saver = tf.train.import_meta_graph(
    #     # './checkpoints/frozenlake.meta')
    #     self.saver.restore(
    #         self.sess, tf.train.latest_checkpoint('./checkpoints'))

    #     t = -1
    #     s = env.reset()
    #     rAll = 0.
    #     num_episode = 1
    #     for i in range(0, num_episode):
    #         while True:
    #             t += 1
    #             input_val = self._generate_input(t, s)
    #             a, Q = self.sess.run(
    #                 [self.predict, self.Qout],
    #                 feed_dict={self.inputs1: input_val})
    #             s1, r, d = env.step(a[0])
    #             env.render()
    #             print "Q = " + str(Q)
    #             rAll += r
    #             s = s1
    #             if d:
    #                 break

    #     print("average award = " + str(rAll / num_episode))


if __name__ == '__main__':
    NUM_EPISODES = 10000

    # read config file:
    # 0. EXP_NAME
    # 1. T_LIST
    # 2. NUM_LEVEL
    # 3. NUM_TIME_SLOTS
    # 4. NEG_REWARD
    # 5. INPUT_SIZE
    # 6. NUM_EPISODES
    config = None   # using default setting
    if len(sys.argv) == 3 and sys.argv[1] == "--config":
        with open(sys.argv[2]) as config_file:
            config = json.load(config_file)

    if config is not None:
        if "NUM_EPISODES" in config:
            NUM_EPISODES = config["NUM_EPISODES"]

    env = ClefEnv(config=config)
    num_actions = len(env.get_action_space())

    qNet = QNet(40, num_actions, config=config)
    qNet.train(env, num_episodes=NUM_EPISODES)
    env.save_qnet_model()
    # qNet.test(env)

    # total_r = 0
    # while True:
    #     action_idx = np.random.randint(num_actions)
    #     s, r, d = env.step(action_idx)
    #     print "r = %d, d = %d, action = %d" % (r, d, action_idx)
    #     total_r += r
    #     if d:
    #         break
    # print "total_r = " + str(total_r)

from clef_env import ClefEnv

import numpy as np
import tensorflow as tf
import os


class QNet:
    def __init__(self, input_size, num_actions):
        self.input_size = input_size
        self.num_actions = num_actions

        tf.reset_default_graph()
        # These lines establish the feed-forward part of
        # the network used to choose actions
        self.inputs1 = tf.placeholder(shape=[1, input_size], dtype=tf.float32)
        self.W1 = tf.Variable(tf.zeros(
            [input_size, input_size / 2]), name='W1')
        self.b1 = tf.Variable(tf.zeros([input_size / 2]), name='b1')
        z1 = tf.matmul(self.inputs1, self.W1) + self.b1
        y1 = tf.sigmoid(z1)
        self.W2 = tf.Variable(tf.zeros(
            [input_size / 2, num_actions]), name='W2')
        self.b2 = tf.Variable(tf.zeros([num_actions]), name='b2')
        z2 = tf.matmul(y1, self.W2) + self.b2
        self.Qout = tf.sigmoid(z2)
        # self.Qout = tf.matmul(y1, self.W2) + self.b2
        self.predict = tf.argmax(self.Qout, 1)

        # Below we obtain the loss by taking the sum of
        # squares difference between the target and prediction Q values.
        self.nextQ = tf.placeholder(shape=[1, num_actions], dtype=tf.float32)
        self.loss = tf.reduce_sum(tf.square(self.nextQ - self.Qout))
        self.trainer = tf.train.GradientDescentOptimizer(learning_rate=0.1)
        self.updateModel = self.trainer.minimize(self.loss)

        # Initialize table with all zeros
        init = tf.initialize_all_variables()
        self.sess = tf.Session()
        self.sess.run(init)
        self.saver = tf.train.Saver()

    def _generate_input(self, t, status):
        if t == 0:
            return [[0] * self.input_size]
        if t > 0 and t < self.input_size:
            input_val = [0] * self.input_size
            input_val[self.input_size - t:] = status[:t]
        else:
            input_val = status[t - self.input_size:t]
        return [input_val]

    def train(self, env, num_episodes=2000):
        # Set learning parameters
        y = .99
        e = 0.3
        # create lists to contain total rewards and steps per episode
        tList = []
        rList = []

        for i in range(num_episodes):
            print "episode " + str(i)
            # Reset environment and get first new observation
            s = env.reset()
            rAll = 0
            d = False
            t = -1
            # The Q-Table learning algorithm
            while True:
                # print t
                t += 1
                # Choose an action by greedily (with e chance of random action)
                # from the Q-network
                # input_val = [s]
                input_val = self._generate_input(t, s)

                a, allQ = self.sess.run(
                    [self.predict, self.Qout],
                    feed_dict={self.inputs1: input_val})
                if np.random.rand(1) < e:
                    a[0] = np.random.randint(self.num_actions)

                print allQ

                # Get new state and reward from environment
                s1, r, d = env.step(a[0])
                # input_val1 = [s1]
                input_val1 = self._generate_input(t + 1, s1)

                # Obtain the Q' values by feeding the new state
                # through our network
                Q1 = self.sess.run(
                    self.Qout,
                    feed_dict={self.inputs1: input_val1})
                # Obtain maxQ' and set our target value for chosen action.
                maxQ1 = np.max(Q1)
                targetQ = allQ
                targetQ[0, a[0]] = r + y * maxQ1

                # Test ######
                # R = 0
                # if d:
                #     R = rAll + r
                # targetQ[0, a[0]] = R / 8. + y * maxQ1
                ##################

                # Train our network using target and predicted Q values
                self.sess.run(
                    [self.updateModel],
                    feed_dict={self.inputs1: input_val, self.nextQ: targetQ})
                rAll += r
                s = s1
                if d:
                    # Reduce chance of random action as we train the model.
                    e = 1. / ((i / 100) + 10)
                    break
            tList.append(t)
            rList.append(rAll)
            if i % 100 == 0:
                print "Average damage in an episode:"\
                    + str(sum(rList) / (i + 1))

        # save the model
        if not os.path.exists('./checkpoints'):
            os.makedirs('./checkpoints')
        self.saver.save(self.sess, './checkpoints/clef_optimal_atk')

        print "Final Q-Table Values"
        print "W1 = " + str(self.sess.run(self.W1))
        print "b1 = " + str(self.sess.run(self.b1))
        print "W2 = " + str(self.sess.run(self.W2))
        print "b2 = " + str(self.sess.run(self.b2))

    def test(self, env):
        # run the game with trained Q table:
        # new_saver = tf.train.import_meta_graph(
        # './checkpoints/frozenlake.meta')
        self.saver.restore(
            self.sess, tf.train.latest_checkpoint('./checkpoints'))

        t = -1
        s = env.reset()
        rAll = 0.
        num_episode = 1
        for i in range(0, num_episode):
            while True:
                t += 1
                input_val = self._generate_input(t, s)
                a, Q = self.sess.run(
                    [self.predict, self.Qout],
                    feed_dict={self.inputs1: input_val})
                s1, r, d = env.step(a[0])
                env.render()
                print "Q = " + str(Q)
                rAll += r
                s = s1
                if d:
                    break

        print("average award = " + str(rAll / num_episode))


if __name__ == '__main__':
    env = ClefEnv()
    num_actions = len(env.get_action_space())

    qNet = QNet(12, num_actions)
    qNet.train(env, num_episodes=2000)
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

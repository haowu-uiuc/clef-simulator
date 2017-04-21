
class ValidChecker:
    def __init__(self, Ts, num_levels):
        self.Ts = Ts
        self.num_levels = num_levels

    def is_detectable(self, traffic, period=None):
        start = 0
        if period is not None:
            start = len(traffic) - period
        for t in range(start, len(traffic)):
            if self.is_detectable_at_t(traffic, t):
                return True
        return False

    def is_detectable_at_t(self, traffic, t):
        for T in self.Ts:
            if self._is_detectable_by_T(traffic, T, t):
                return True
        return False

    def _is_detectable_by_T(self, traffic, T, t):
        for start in range(
                t - T * self.num_levels + 1,
                t - T * (self.num_levels - 1) + 1):
            probs = [0] * self.num_levels
            for i in range(start, start + self.num_levels * T):
                if i < 0:
                    continue
                if i >= len(traffic):
                    break
                if traffic[i] == 1:
                    probs[(i - start) / T] = 1
                if (i - start + 1) % T == 0 and probs[(i - start) / T] == 0:
                    # (start, T) cannot detect this traffic
                    break
            if sum(probs) == self.num_levels:
                return True
        return False


def render_traffic(traffic):
    for i in range(0, len(traffic)):
        if traffic[i] == 1:
            print "%d ->\t [*]" % i
        else:
            print "%d ->\t [ ]" % i
    print "rate = " + str(float(sum(traffic)) / len(traffic))
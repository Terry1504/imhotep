package com.indeed.imhotep.metrics.aggregate;

import com.google.common.base.Objects;

/**
 * @author jwolfe
 */
public class AggregatePower implements AggregateStat {
    private final AggregateStat lhs;
    private final AggregateStat rhs;

    public AggregatePower(AggregateStat lhs, AggregateStat rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public double apply(MultiFTGSIterator multiFTGSIterator) {
        return Math.pow(lhs.apply(multiFTGSIterator), rhs.apply(multiFTGSIterator));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregatePower that = (AggregatePower) o;
        return Objects.equal(lhs, that.lhs) &&
                Objects.equal(rhs, that.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lhs, rhs);
    }

    @Override
    public String toString() {
        return "AggregatePower{" +
                "lhs=" + lhs +
                ", rhs=" + rhs +
                '}';
    }
}

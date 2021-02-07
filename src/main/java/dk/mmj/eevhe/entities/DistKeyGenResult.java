package dk.mmj.eevhe.entities;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DistKeyGenResult {
    private BigInteger g;
    private BigInteger q;
    private BigInteger p;
    private List<Integer> authorityIds;
    private Map<Integer, BigInteger> secretValues;
    private Map<Integer, BigInteger> publicValues;

    /**
     * Unused object mapper constructor
     */
    @SuppressWarnings("unused")
    private DistKeyGenResult() {
    }

    public DistKeyGenResult(BigInteger g, BigInteger q, Map<Integer, BigInteger> secretValues, Map<Integer, BigInteger> publicValues) {
        this.g = g;
        this.q = q;
        this.p = q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE);
        this.secretValues = secretValues;
        this.publicValues = publicValues;
        authorityIds = new ArrayList<>(secretValues.keySet());
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getQ() {
        return q;
    }

    public BigInteger getP() {
        return p;
    }

    public List<Integer> getAuthorityIds() {
        return authorityIds;
    }

    public Map<Integer, BigInteger> getSecretValues() {
        return secretValues;
    }

    public Map<Integer, BigInteger> getPublicValues() {
        return publicValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistKeyGenResult that = (DistKeyGenResult) o;
        return Objects.equals(g, that.g) &&
                Objects.equals(q, that.q) &&
                Objects.equals(p, that.p) &&
                Objects.equals(authorityIds, that.authorityIds) &&
                Objects.equals(secretValues, that.secretValues) &&
                Objects.equals(publicValues, that.publicValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(g, q, p, authorityIds, secretValues, publicValues);
    }
}

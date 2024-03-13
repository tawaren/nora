package nora.compiler.entries.resolved;

public enum Variance {
    Covariance,
    Contravariance,
    Invariance,
    Bivariance;

    public boolean canBeAppliedTo(Variance var){
        return switch (this){
            case Invariance -> true;
            case Covariance -> var == Covariance || var == Bivariance;
            case Contravariance -> var == Contravariance || var == Bivariance;
            case Bivariance -> var == Bivariance;
        };
    }

    public Variance flip(){
        return switch (this){
            case Invariance -> Invariance;
            case Covariance -> Contravariance;
            case Contravariance -> Covariance;
            case Bivariance -> Bivariance;
        };
    }

    public Variance flipBy(Variance cond){
        return switch (cond){
            case Invariance -> Invariance;
            case Covariance -> this;
            case Contravariance -> flip();
            case Bivariance -> this == Invariance? Invariance:Bivariance;
        };
    }

    public String generateVarianceSign(boolean printInvariance){
        if(this == Bivariance)System.out.println("Warning BiVariance survived to serializer it is coerced into Contravariance");
        String invarianceSign;
        if(printInvariance){
            invarianceSign = "~";
        } else {
            invarianceSign = ""; //No sign = Invariance
        }
        return switch (this){
            case Invariance -> invarianceSign;
            case Covariance -> "+";
            case Contravariance -> "-";
            case Bivariance -> "-"; //Is not supported fallback to contra (as it is mostly used in dynamic arguments where contra feels more natural)
        };
    }

    public String generateVarianceSign(){
        return generateVarianceSign(false);
    }

}

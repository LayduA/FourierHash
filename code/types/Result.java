package code.types;

public class Result {
    private Distance distance;
    private double correction, compressionRate, similarity;

    public Result(Distance dist, double corr, double comp, double sim) {
        compressionRate = comp;
        similarity = sim;
        correction = corr;
        distance = dist;
    }

    public double getCompressionRate() {
        return compressionRate;
    }

    public double getSimilarity() {
        return similarity;
    }

    public Distance getDistance() {
        return distance;
    }

    public double getCorrection() {
        return correction;
    }

    @Override
    public String toString() {
        return  "{\n" +
                "\t\"distance\": \"" + distance + "\", \n" +
                "\t\"correction\": " + correction + ",\n" +
                "\t\"compressionRate\": " + compressionRate + ",\n" +
                "\t\"similarity\": " + similarity + "\n"
                + "}";
    }
}

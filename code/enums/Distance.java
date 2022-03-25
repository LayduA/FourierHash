package code.enums;

public enum Distance {
    BELL, MANHATTAN, EUCLIDEAN, CUBIC, MULT, SQUARE, XY, SQRTMIN, SIGMOID;

    public double dist(double x, double y, double cut) {

        if (x > 128) return dist(256 - x, y, cut);
        double minXY = Math.min(Math.abs(x), Math.abs(y));
        double maxXY = Math.max(Math.abs(x), Math.abs(y));
        double sumXY = Math.abs(x) + Math.abs(y);

        if (maxXY > cut) return 0;

        if (this == SQRTMIN) {
            if (x == 0 && y == 0) return 1.0;
            return 0.5 * (x == 0 || y == 0 ? 2 : 1 / Math.pow(minXY, 0.2));
        }
        if (this == SIGMOID) {
            double l = 1 - (2 * maxXY) / (2 * cut + 1);

            return l / (1 + Math.exp(-(-maxXY * 0.001) * (-minXY)));
        }
        if (this == MULT) return x * y == 0 ? 4 : 2 / Math.abs(x * y);
        if (this == MANHATTAN) return (sumXY) == 0 ? 4 : 2 / (sumXY);
        double dist = Math.sqrt(x * x + y * y);
        if (this == EUCLIDEAN) return dist == 0 ? 4 : 2 / dist;
        if (this == BELL) return Math.exp(-0.5 * dist * dist / 1.5);
        if (this == CUBIC) {
            double cube = (Math.abs(x) * x * x + Math.abs(y) * y * y);
            return cube == 0 ? 4 : 2 / cube;
        }
        double squareDist = (x*x + y*y);
        if (this == SQUARE) return squareDist == 0 ? 4 : 2 / squareDist;
        if (this == XY) return squareDist == 0 ? 4 : x * y * 2.0 / squareDist;
        return 0;
    }
}

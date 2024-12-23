package src.types;

public enum Distance {
    CONST, MIN, MANHATTAN, EUCLIDEAN, CUBIC, MULT, SQUARE, XY, SQRTMIN, BELL,  SIGMOID;

    public enum Shape{
        RECTANGLE, RHOMBUS
    }

    public double dist(double x, double y, DrawParams params) {

        if (x > 128) return dist(256 - x, y, params);

        double minXY = Math.min(Math.abs(x), Math.abs(y));
        double maxXY = Math.max(Math.abs(x), Math.abs(y));
        double sumXY = Math.abs(x) + Math.abs(y);

        //square
        if (params.getDistMode() == Shape.RECTANGLE && maxXY > params.cut()) return 0;
        if (params.getDistMode() == Shape.RHOMBUS && sumXY > params.cut()) return 0;
        if(this == MIN) return minXY == 0 ? 4 : 2 / minXY;
        if(this == CONST) return 2.0;
        if (this == SQRTMIN) {
            if (x == 0 && y == 0) return 1.0;
            return 0.5 * (x == 0 || y == 0 ? 1.2 : 1 / Math.sqrt(minXY));
        }
        if (this == SIGMOID) {
            double l = 1 - (2 * maxXY) / (2 * params.cut() + 1);

            return l / (1 + Math.exp(-(-maxXY * 0.01) * (-minXY)));
        }
        if (this == MULT) return x * y == 0 ? 1 : 1 / Math.abs(x * y);
        if (this == MANHATTAN) return (sumXY) == 0 ? 2 : 2 / (sumXY);
        double dist = Math.sqrt(x * x + y * y);
        if (this == EUCLIDEAN) return dist == 0 ? 2 : 1 / dist;
        if (this == BELL) return Math.exp(-0.5 * dist * dist / 1.5);
        if (this == CUBIC) {
            double cube = (Math.abs(x) * x * x + Math.abs(y) * y * y);
            return cube == 0 ? 2 : 1 / cube;
        }
        double squareDist = (x * x + y * y);
        if (this == SQUARE) return squareDist == 0 ? 1 : 1 / squareDist;
        if (this == XY) return squareDist == 0 ? 2 : x == 0 || y == 0 ? (x+y) / squareDist : x * y * 1.0 / squareDist;
        return 0;
    }
}

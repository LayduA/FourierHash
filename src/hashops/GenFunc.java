package src.hashops;

import java.util.Arrays;

import src.crypto.PRNG128BitsInsecure;

public class GenFunc {
    public static enum TYPES {
        CONST, X, Y, ADD, SUB, MULT, DIV, MOD, SIN, COS, SQRT, EXP, MIX;

        public int arity() {
            switch (this) {
                case CONST:
                case X:
                case Y:
                    return 0;
                case SIN:
                case COS:
                case SQRT:
                case EXP:
                    return 1;
                case ADD:
                case SUB:
                case MULT:
                case DIV:
                case MOD:
                    return 2;
                case MIX:
                    return 4;
                default:
                    return -1;
            }
        }

    }
    public static TYPES[] TERMINAL = {TYPES.CONST, TYPES.X, TYPES.Y};
    public static TYPES[] NON_TERMINAL = {TYPES.ADD, TYPES.SUB, TYPES.MULT, TYPES.DIV, TYPES.MOD, TYPES.SIN, TYPES.COS, TYPES.SQRT, TYPES.EXP, TYPES.MIX};

    // private class Vec3 {

    //     private double x;
    //     private double y;
    //     private double z;

    //     public Vec3(double x, double y, double z) {
    //         this.x = x;
    //         this.y = y;
    //         this.z = z;
    //     }

    //     public Vec3 op(TYPES t, double coordX, double coordY, Vec3... others) {

    //         switch (t) {
    //             case CONST:
    //                 return this;
    //             case X:

    //             case Y:
    //                 return 0;
    //             case SIN:
    //             case COS:
    //             case SQRT:
    //             case EXP:
    //                 return 1;
    //             case ADD:
    //             case SUB:
    //             case MULT:
    //             case DIV:
    //             case MOD:
    //                 return 2;
    //             case MIX:
    //                 return 4;
    //             default:
    //                 return -1;
    //         }

    //     }
    // }

    public GenFunc[] f_args;

    public GenFunc(PRNG128BitsInsecure b) {
        this(TYPES.CONST, b);
    }

    public GenFunc(TYPES t, PRNG128BitsInsecure b, GenFunc... args) {
        type = t;
        this.value = b.rand() * 2 - 1;
        f_args = Arrays.copyOf(args, args.length);
    }

    private TYPES type;

    private double value;

    public static double cap(double val, double low, double high) {
        if (val > high)
            return high;
        return val < low ? low : val;
    }

    public double eval(double x, double y) {
        if (x < -1.0 || x > 1.0 || y < -1.0 || y > 1.0) {
            System.out.println("error in function of type " + type.name());
            return 0;
        }
        switch (type) {
            case CONST:
                return value;
            case X:
                return x;
            case Y:
                return y;
            case ADD:
                return (f_args[0].eval(x, y) + f_args[1].eval(x, y)) / 2.0;
            case SUB:
                return (f_args[0].eval(x, y) - f_args[1].eval(x, y)) / 2.0;
            case MULT:
                return f_args[0].eval(x, y) * f_args[1].eval(x, y);
            case DIV:
                if (f_args[1].eval(x, y) == 0.0)
                    return 1.0;
                return cap(f_args[0].eval(x, y) / f_args[1].eval(x, y), -1.0, 1.0);
            case MOD:
                if (f_args[1].eval(x, y) == 0.0)
                    return 1.0;
                return f_args[0].eval(x, y) % f_args[1].eval(x, y);
            case SIN:
                return Math.sin(Math.PI * f_args[0].eval(x, y));
            case COS:
                return Math.cos(Math.PI * f_args[0].eval(x, y));
            case SQRT:
                return Math.sqrt(2) * Math.sqrt(f_args[0].eval(x, y) + 1.0) - 1.0;
            case EXP:
                return cap(Math.exp(f_args[0].eval(x, y)) - Math.exp(-1) - 1.0, -1.0, 1.0);
            case MIX:
                return cap(
                        (f_args[0].eval(x, y) * f_args[1].eval(x, y) + f_args[0].eval(x, y) * f_args[1].eval(x, y)) / 2,
                        -1.0, 1.0);
            default:
                return 0;
        }
    }

    @Override
    public String toString() {
        if (type == TYPES.CONST)
            return value + "";
        switch (type) {
            case CONST:
                return value + "";
            case ADD:
            case SUB:
            case MULT:
            case DIV:
            case MOD:
                return type.name() + "(" + f_args[0].toString() + "," + f_args[1].toString() + ")";
            case X:
            case Y:
                return type.name().toLowerCase();
            case SIN:
            case COS:
            case SQRT:
            case EXP:
                return type.name() + "(" + f_args[0].toString() + ")";
            case MIX:
                return type.name() + "(" + f_args[0].toString() + "," + f_args[1].toString() + ","
                        + f_args[2].toString() + "," + f_args[3].toString() + ")";
            default:
                return "jui bet";
        }

    }

    public static GenFunc randomFunc(String seed, int depth) {
        PRNG128BitsInsecure b = new PRNG128BitsInsecure(seed);

        return randomFunc(depth, b);
    }

    public static GenFunc randomFunc(int depth, PRNG128BitsInsecure b) {
        if (depth == 0)
            return new GenFunc(TERMINAL[(int) Math.floor(b.rand() * TERMINAL.length)], b);

        TYPES type = NON_TERMINAL[(int) Math.floor(b.rand() * NON_TERMINAL.length)];
        GenFunc[] args = new GenFunc[type.arity()];
        for (int i = 0; i < args.length; i++) {
            args[i] = randomFunc(depth - 1, b);
        }
        return new GenFunc(type, b, args);
    }

    // public static void main(String[] args) {
    //     System.out.println(randomFunc("pndenjjjjjjjjjjjjjjjjjjjjjor", 7));

    // }
}